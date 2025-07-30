package org.example.aiedabackend.service.serviceimpl;

import org.example.aiedabackend.constant.MessageTypeConstant;
import org.example.aiedabackend.dao.RecordRepository;
import org.example.aiedabackend.dao.SessionRepository;
import org.example.aiedabackend.po.Record;
import org.example.aiedabackend.po.Session;
import org.example.aiedabackend.service.ChatService;
import org.example.aiedabackend.vo.RecordVO;
import org.example.aiedabackend.vo.SessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private RecordRepository recordRepository;

    private RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<SessionVO> getSessions(Integer uid) {
        List<Session> sessions = sessionRepository.findByUid(uid);
        // 按updateTime倒序排列，确保最新活跃的会话显示在最上面
        return sessions.stream()
                .sorted((s1, s2) -> s2.getUpdateTime().compareTo(s1.getUpdateTime()))
                .map(Session::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 更新会话的最后活跃时间
     * 在发送消息时调用，确保最新发送消息的会话显示在列表最上面
     */
    private void updateSessionTime(Integer uid, Integer sid) {
        try {
            var optional = sessionRepository.findById(sid);
            if (optional.isPresent()) {
                Session session = optional.get();
                // 验证会话属于该用户
                if (session.getUid().equals(uid)) {
                    session.setUpdateTime(LocalDateTime.now());
                    sessionRepository.save(session);
                    log.info("更新会话{}的活跃时间", sid);
                } else {
                    log.warn("用户{}尝试更新不属于自己的会话{}", uid, sid);
                }
            } else {
                log.warn("尝试更新不存在的会话{}", sid);
            }
        } catch (Exception e) {
            log.error("更新会话时间失败，sid: {}, uid: {}", sid, uid, e);
        }
    }

    @Override
    public SessionVO createSession(Integer uid, String title) {
        LocalDateTime now = LocalDateTime.now();
        Session session = new Session(uid, title, now, now);
        session = sessionRepository.save(session);
        return session.toVO();
    }

    @Override
    public List<RecordVO> getRecords(Integer sid) {
        List<Record> records = recordRepository.findBySidOrderBySequenceAsc(sid);
        return records.stream().map(Record::toVO).collect(Collectors.toList());
    }

    @Override
    public boolean updateSessionTitle(Integer uid, Integer sid, String title) {
        var optional = sessionRepository.findById(sid);
        if (optional.isPresent() && optional.get().getUid().equals(uid)) {
            Session session = optional.get();
            session.setTitle(title);
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.save(session);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public boolean deleteSession(Integer uid, Integer sid) {
        var optional = sessionRepository.findById(sid);
        if (optional.isPresent() && optional.get().getUid().equals(uid)) {
            // 删除所有关联的记录
            recordRepository.deleteBySid(sid);
            // 删除会话
            sessionRepository.deleteById(sid);
            return true;
        }
        return false;
    }

    @Override
    public SseEmitter sendMessageSSE(Integer uid, Integer sid, String content) {
        // 创建SSE发射器，设置60秒超时
        SseEmitter emitter = new SseEmitter(60000L);

        // 更新会话时间，确保最新发送消息的会话显示在最上面
        updateSessionTime(uid, sid);

        // 保存用户消息
        List<Record> existing = recordRepository.findBySidOrderBySequenceAsc(sid);
        int nextSeq = existing.isEmpty() ? 1 : existing.size() + 1;
        LocalDateTime now = LocalDateTime.now();
        Record userRecord = new Record(sid, uid, true, content, nextSeq, MessageTypeConstant.USER, now);
        recordRepository.save(userRecord);

        // 异步处理SSE流
        CompletableFuture.runAsync(() -> {
            processAIStreamResponse(emitter, uid, sid, content, nextSeq);
        });

        // 设置超时和错误处理
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
            emitter.complete();
        });

        emitter.onError((throwable) -> {
            log.error("SSE连接错误", throwable);
            emitter.complete();
        });

        return emitter;
    }

    /**
     * 处理AI流式回复的完整流程
     */
    private void processAIStreamResponse(SseEmitter emitter, Integer uid, Integer sid, String content, int nextSeq) {
        StringBuilder aiReply = new StringBuilder();
        String userInputUrl = "http://localhost:8000/api/v1/user/input";

        try {
            // 1. 提交用户输入
            var inputRequest = new java.util.HashMap<String, Object>();
            inputRequest.put("session_id", sid.toString());
            inputRequest.put("user_id", uid.toString());
            inputRequest.put("input_text", content);
            inputRequest.put("input_type", "text");

            var inputResponse = restTemplate.postForObject(userInputUrl, inputRequest, java.util.Map.class);
            if (inputResponse == null || !"success".equals(inputResponse.get("status"))) {
                sendErrorToFrontend(emitter, "提交用户输入失败");
                return;
            }

            // 立即发送开始信号
            sendMessageToFrontend(emitter, "start", "AI正在思考...", null);

            // 2. 立即开始获取AI流式回复，实时转发
            String sessionId = sid.toString();
            String finalReply = getAIStreamResponse(emitter, sessionId, aiReply);

            // 保存完整的AI回复
            Record aiRecord = null;
            if (finalReply != null && !finalReply.trim().isEmpty() && !finalReply.equals("AI回复为空")) {
                aiRecord = new Record(sid, uid, false, finalReply, nextSeq + 1,
                    MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
                aiRecord = recordRepository.save(aiRecord);
                System.out.println("AI回复已保存，记录ID: " + aiRecord.getRid() + "，内容长度: " + finalReply.length());
            } else {
                System.out.println("AI回复为空或无效，将使用默认回复");
                // 如果没有有效回复，创建一个默认回复
                String defaultReply = aiReply.length() > 0 ? aiReply.toString() : "AI回复为空";
                aiRecord = new Record(sid, uid, false, defaultReply, nextSeq + 1,
                    MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
                aiRecord = recordRepository.save(aiRecord);
            }

            // 无论如何都要发送完成信号
            int recordId = aiRecord != null ? aiRecord.getRid() : -1;
            sendCompleteToFrontend(emitter, "回复完成", recordId);
            System.out.println("流式处理完成，已发送complete事件");

        } catch (Exception e) {
            log.error("SSE流式处理失败", e);
            handleProcessError(emitter, e, uid, sid, nextSeq);
        } finally {
            // 确保连接被正确关闭
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("关闭SSE连接失败", e);
            }
        }
    }

    /**
     * 获取AI流式回复
     */
    private String getAIStreamResponse(SseEmitter emitter, String sessionId, StringBuilder aiReply) throws Exception {
        String sseStreamUrl = "http://localhost:8000/api/v1/stream";
        boolean generationFinished = false;
        boolean[] foundFinishStart = {false}; // 使用数组来保持状态
        StringBuilder fullDelta = new StringBuilder(); // 用于累积不完整的标签

        // 建立SSE连接
        URL url = new URL(sseStreamUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setConnectTimeout(10000); // 10秒连接超时
        connection.setReadTimeout(60000); // 60秒读取超时

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), "UTF-8"))) {

            String line;
            StringBuilder currentEvent = new StringBuilder();
            
            while ((line = reader.readLine()) != null && !generationFinished) {
                // 处理SSE数据行
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6); // 移除"data: "前缀

                    if (jsonData.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        // 解析并处理事件数据，立即转发到前端
                        generationFinished = processSSEEventData(emitter, jsonData, sessionId,
                            aiReply, fullDelta, foundFinishStart);

                    } catch (Exception parseError) {
                        log.warn("解析SSE事件数据失败: {}", jsonData, parseError);
                        // 即使解析失败，也尝试继续处理其他事件
                    }
                } else if (line.trim().isEmpty()) {
                    // 空行表示事件结束，可以在这里做一些清理工作
                    continue;
                } else {
                    // 处理其他SSE头部信息（如event:, id:等）
                    System.out.println("SSE头部信息: " + line);
                }
                
                // 确保及时响应前端，避免缓冲延迟
                if (aiReply.length() > 0) {
                    // 每隔一定数量的字符就主动flush一次
                    if (aiReply.length() % 50 == 0) {
                        try {
                            // 发送心跳或者刷新连接
                            Thread.sleep(10); // 短暂延迟确保数据传输
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        return aiReply.length() > 0 ? aiReply.toString() : "AI回复为空";
    }

    /**
     * 处理SSE事件数据
     */
    private boolean processSSEEventData(SseEmitter emitter, String jsonData, String sessionId,
                                       StringBuilder aiReply, StringBuilder fullDelta, boolean[] foundFinishStart) throws Exception {

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> eventData = mapper.readValue(jsonData, java.util.Map.class);

        // 打印接收到的事件数据用于调试
        System.out.println("接收到SSE事件: " + jsonData);

        // 检查是否是连接确认消息
        if ("connection".equals(eventData.get("type"))) {
            log.info("SSE连接已建立: {}", eventData.get("message"));
            return false;
        }

        // 处理事件类型数据
        if ("event".equals(eventData.get("type"))) {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) eventData.get("data");
            if (data != null) {
                @SuppressWarnings("unchecked")
                var innerData = (java.util.Map<String, Object>) data.get("data");
                if (innerData != null && Boolean.TRUE.equals(innerData.get("batch"))) {
                    // 处理批量事件
                    @SuppressWarnings("unchecked")
                    var events = (java.util.List<java.util.Map<String, Object>>) innerData.get("events");
                    if (events != null) {
                        for (var event : events) {
                            if (processSingleEvent(emitter, event, sessionId, aiReply, fullDelta, foundFinishStart)) {
                                return true; // 生成完成
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 处理单个事件
     */
    private boolean processSingleEvent(SseEmitter emitter, java.util.Map<String, Object> event, String sessionId,
                                     StringBuilder aiReply, StringBuilder fullDelta, boolean[] foundFinishStart) throws Exception {

        String eventSessionId = (String) event.get("session_id");
        String userId = (String) event.get("user_id");
        String eventType = (String) event.get("event");
        String tool = (String) event.get("tool");

        // 打印事件详情到控制台
        System.out.println(String.format("事件详情 - session_id: %s, user_id: %s, event: %s, tool: %s",
            eventSessionId, userId, eventType, tool));

        // 检查是否是结束事件（after + orchestrator + result.message = "FINISH"）
        if (sessionId.equals(eventSessionId) && "after".equals(eventType) && "orchestrator".equals(tool)) {
            // 检查result字段
            @SuppressWarnings("unchecked")
            var result = (java.util.Map<String, Object>) event.get("result");
            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                String message = (String) result.get("message");
                if ("FINISH".equals(message)) {
                    log.info("检测到result.message=FINISH，AI回复生成完成");
                    return true; // 生成完成
                }
            }
        }

        // 处理流式输出事件（llm_tool.chunk）
        if (sessionId.equals(eventSessionId) && "llm_tool.chunk".equals(eventType)) {
            @SuppressWarnings("unchecked")
            var payload = (java.util.Map<String, Object>) event.get("payload");
            if (payload != null && payload.get("delta") != null) {
                String delta = payload.get("delta").toString();

                // 打印delta内容
                System.out.println("Delta内容: " + delta);

                if (!delta.isEmpty()) {
                    // 处理delta内容，过滤finish标签但保留其他标签
                    String processedDelta = processAndFilterDelta(delta, fullDelta);
                    if (!processedDelta.isEmpty()) {
                        // 发送处理后的内容到前端
                        sendDeltaToFrontend(emitter, processedDelta);
                        aiReply.append(processedDelta);
                        System.out.println("发送到前端的内容: " + processedDelta);
                    }
                }
            }
        }

        return false;
    }

    /**
     * 处理并过滤delta内容
     * 等待完整标签出现，过滤finish标签，保留其他标签
     */
    private String processAndFilterDelta(String delta, StringBuilder fullDelta) {
        fullDelta.append(delta);
        String accumulated = fullDelta.toString();
        StringBuilder result = new StringBuilder();
        StringBuilder remaining = new StringBuilder();
        
        int i = 0;
        while (i < accumulated.length()) {
            char c = accumulated.charAt(i);
            
            if (c == '<') {
                // 寻找完整的标签
                int tagStart = i;
                int tagEnd = findCompleteTag(accumulated, i);
                
                if (tagEnd == -1) {
                    // 标签不完整，保留到remaining中等待更多内容
                    remaining.append(accumulated.substring(i));
                    break;
                } else {
                    // 找到完整标签
                    String tag = accumulated.substring(tagStart, tagEnd + 1);
                    
                    // 检查是否是finish标签
                    if (tag.matches("(?i)</?finish>")) {
                        // 是finish标签，跳过不添加到结果中
                        System.out.println("过滤finish标签: " + tag);
                    } else {
                        // 不是finish标签，添加到结果中
                        result.append(tag);
                        System.out.println("保留标签: " + tag);
                    }
                    i = tagEnd + 1;
                }
            } else {
                // 普通字符，直接添加
                result.append(c);
                i++;
            }
        }
        
        // 更新fullDelta为剩余未处理的内容
        fullDelta.setLength(0);
        fullDelta.append(remaining);
        
        return result.toString();
    }

    /**
     * 寻找完整标签的结束位置
     */
    private int findCompleteTag(String text, int startPos) {
        if (startPos >= text.length() || text.charAt(startPos) != '<') {
            return -1;
        }
        
        // 寻找对应的'>'
        int pos = startPos + 1;
        boolean inQuotes = false;
        char quoteChar = 0;
        
        while (pos < text.length()) {
            char c = text.charAt(pos);
            
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
            } else if (!inQuotes && c == '>') {
                return pos; // 找到标签结束
            }
            
            pos++;
        }
        
        return -1; // 标签不完整
    }

    /**
     * 处理finish标签内的内容，清理JSON格式等
     */
    private String processFinishContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        // 尝试解析JSON格式的内容
        try {
            // 检查是否是JSON格式
            if (content.trim().startsWith("{") && content.trim().endsWith("}")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> json = mapper.readValue(content.trim(), java.util.Map.class);

                Object answer = json.get("answer");
                if (answer != null) {
                    return answer.toString();
                }
            }

            // 如果不是完整的JSON，尝试提取JSON片段
            String cleaned = content
                .replaceAll("\\{\"answer\":\"?", "")  // 移除answer开始部分
                .replaceAll("\"?\\}$", "")  // 移除结尾的引号和大括号
                .trim();

            return cleaned;

        } catch (Exception e) {
            // JSON解析失败，返回清理后的文本
            System.out.println("JSON解析失败，返回清理后的文本: " + content);
            return content.trim();
        }
    }

    /**
     * 发送错误信息到前端
     */
    private void sendErrorToFrontend(SseEmitter emitter, String errorMessage) {
        try {
            String errorJson = String.format(
                "{\"type\":\"error\",\"message\":\"%s\"}",
                errorMessage.replace("\"", "\\\""));
            emitter.send(SseEmitter.event().name("error").data(errorJson));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }

    /**
     * 发送消息到前端
     */
    private void sendMessageToFrontend(SseEmitter emitter, String type, String message, Object extraData) {
        try {
            String messageJson;
            if (extraData != null) {
                messageJson = String.format(
                    "{\"type\":\"%s\",\"message\":\"%s\",\"data\":%s}",
                    type, message.replace("\"", "\\\""), extraData.toString());
            } else {
                messageJson = String.format(
                    "{\"type\":\"%s\",\"message\":\"%s\"}",
                    type, message.replace("\"", "\\\""));
            }
            emitter.send(SseEmitter.event().name(type).data(messageJson));
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 发送增量内容到前端
     */
    private void sendDeltaToFrontend(SseEmitter emitter, String deltaContent) {
        try {
            // 清理和转义内容
            String cleanContent = deltaContent
                .replace("\\", "\\\\")   // 转义反斜杠
                .replace("\"", "\\\"")   // 转义双引号
                .replace("\n", "\\n")    // 转义换行符
                .replace("\r", "\\r")    // 转义回车符
                .replace("\t", "\\t");   // 转义制表符
            
            String deltaJson = String.format(
                "{\"type\":\"delta\",\"content\":\"%s\"}",
                cleanContent);
            
            // 发送SSE事件，使用data事件名
            emitter.send(SseEmitter.event().name("message").data(deltaJson));
            
            // 添加调试日志
            System.out.println("发送SSE增量数据: " + deltaJson);
            
        } catch (Exception e) {
            log.error("发送增量内容失败: " + deltaContent, e);
            // 尝试发送错误信息
            try {
                sendErrorToFrontend(emitter, "发送内容时出错: " + e.getMessage());
            } catch (Exception errorEx) {
                log.error("发送错误信息也失败了", errorEx);
            }
        }
    }

    /**
     * 发送完成信号到前端
     */
    private void sendCompleteToFrontend(SseEmitter emitter, String message, int recordId) {
        try {
            String completeJson = String.format(
                "{\"type\":\"complete\",\"message\":\"%s\",\"recordId\":%d}",
                message, recordId);
            
            System.out.println("准备发送complete事件: " + completeJson);
            emitter.send(SseEmitter.event().name("complete").data(completeJson));
            System.out.println("complete事件已发送");
            
        } catch (Exception e) {
            log.error("发送完成信号失败", e);
        }
    }

    /**
     * 处理流程错误
     */
    private void handleProcessError(SseEmitter emitter, Exception e, Integer uid, Integer sid, int nextSeq) {
        try {
            // 保存错误消息
            String errMsg = "AI回复失败: " + e.getMessage();
            Record errRecord = new Record(sid, uid, false, errMsg, nextSeq + 1,
                MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
            recordRepository.save(errRecord);

            // 发送错误信息
            sendErrorToFrontend(emitter, e.getMessage());
        } catch (Exception sendError) {
            log.error("处理错误失败", sendError);
        }
    }

    /**
     * 从完整的delta字符串中提取finish标签内的答案内容
     */
    private String extractContentFromDelta(String deltaStr) {
        try {
            // 查找<finish>标签内的内容
            int startIndex = deltaStr.indexOf("<finish>");
            int endIndex = deltaStr.indexOf("</finish>");
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                // 提取<finish>标签内的内容
                String finishContent = deltaStr.substring(startIndex + 8, endIndex).trim();
                System.out.println("提取到finish标签内容: " + finishContent);
                
                // 解析JSON，提取answer字段
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> json = mapper.readValue(finishContent, java.util.Map.class);
                    
                    Object answer = json.get("answer");
                    if (answer != null) {
                        String answerContent = answer.toString();
                        System.out.println("提取到answer内容: " + answerContent);
                        return answerContent;
                    }
                } catch (Exception jsonError) {
                    System.out.println("JSON解析失败，返回原始内容: " + finishContent);
                    // 如果JSON解析失败，返回原始内容
                    return finishContent;
                }
            }
            
            // 如果没有找到finish标签，直接返回累积的delta内容
            // 去除可能的JSON格式字符和标签
            String cleanedContent = deltaStr
                .replaceAll("</?finish>", "")  // 移除finish标签
                .replaceAll("\\{\"answer\":\"?", "")  // 秼除answer开始部分
                .replaceAll("\"?\\}$", "")  // 移除结尾的引号和大括号
                .trim();
                
            return cleanedContent;
            
        } catch (Exception e) {
            log.warn("提取delta内容失败: " + deltaStr, e);
            // 如果所有解析都失败，返回清理后的文本
            return deltaStr.replaceAll("[<>{}\"\\[\\]]", "").trim();
        }
    }
}
