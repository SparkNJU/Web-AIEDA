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
        return sessions.stream().map(Session::toVO).collect(Collectors.toList());
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
    public RecordVO sendMessage(Integer uid, Integer sid, String content) {
        // 保存用户消息
        List<Record> existing = recordRepository.findBySidOrderBySequenceAsc(sid);
        int nextSeq = existing.isEmpty() ? 1 : existing.size() + 1;
        LocalDateTime now = LocalDateTime.now();
        Record userRecord = new Record(sid, uid, true, content, nextSeq, MessageTypeConstant.USER, now);
        userRecord = recordRepository.save(userRecord);

        // 调用新的AI服务
        String userInputUrl = "http://localhost:8000/api/v1/user/input";
        String eventsUrl = "http://localhost:8000/api/v1/events";

        try {
            // 1. 提交用户输入
            var inputRequest = new java.util.HashMap<String, Object>();
            inputRequest.put("session_id", sid.toString());
            inputRequest.put("user_id", uid.toString());
            inputRequest.put("input_text", content);
            inputRequest.put("input_type", "text");

            var inputResponse = restTemplate.postForObject(userInputUrl, inputRequest, java.util.Map.class);
            if (inputResponse == null || !"success".equals(inputResponse.get("status"))) {
                throw new RuntimeException("提交用户输入失败");
            }

            // 2. 轮询获取AI回复
            StringBuilder aiReply = new StringBuilder();
            String sessionId = sid.toString();
            long startTime = System.currentTimeMillis();
            long timeout = 30000; // 30秒超时
            boolean generationFinished = false;
            java.util.Set<Integer> processedEventIds = new java.util.HashSet<>();

            while (System.currentTimeMillis() - startTime < timeout && !generationFinished) {
                try {
                    // 获取最新事件
                    String eventsUrlWithParams = eventsUrl + "?session_id=" + sessionId + "&limit=5000";
                    var eventsResponse = restTemplate.getForObject(eventsUrlWithParams, java.util.Map.class);
                    System.out.println("轮询获取AI回复...\n" + eventsResponse);
                    if (eventsResponse != null && "success".equals(eventsResponse.get("status"))) {
                        @SuppressWarnings("unchecked")
                        java.util.List<java.util.Map<String, Object>> events =
                            (java.util.List<java.util.Map<String, Object>>) eventsResponse.get("events");

                        if (events != null) {
                            for (var event : events) {
                                // 获取事件ID，避免重复处理
                                Integer eventId = (Integer) event.get("id");
                                if (eventId != null && processedEventIds.contains(eventId)) {
                                    continue; // 跳过已处理的事件
                                }

                                @SuppressWarnings("unchecked")
                                var data = (java.util.Map<String, Object>) event.get("data");
                                if (data != null) {
                                    // 处理payload中的delta（流式输出）
                                    @SuppressWarnings("unchecked")
                                    var payload = (java.util.Map<String, Object>) data.get("payload");
                                    if (payload != null && payload.get("delta") != null) {
                                        String delta = payload.get("delta").toString();
                                        if (!delta.isEmpty()) {
                                            aiReply.append(delta);
                                            System.out.println("AI回复片段: !!!!\n" + delta);
                                        }
                                    }

                                    // 检查是否有最终结果（根据API文档判断）
                                    @SuppressWarnings("unchecked")
                                    var result = (java.util.Map<String, Object>) data.get("result");
                                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                                        @SuppressWarnings("unchecked")
                                        var resultData = (java.util.Map<String, Object>) result.get("data");
                                        if (resultData != null && "FINISH".equals(resultData.get("message"))) {
                                            System.out.println("AI回复结束标志已检测到，停止轮询");
                                            generationFinished = true;
                                            if (eventId != null) {
                                                processedEventIds.add(eventId);
                                            }
                                            break;
                                        }
                                    }

                                    // 标记事件已处理
                                    if (eventId != null) {
                                        processedEventIds.add(eventId);
                                    }
                                }
                            }
                        }
                    }

                    // 如果已完成生成，退出循环
                    if (generationFinished) {
                        break;
                    }

                    Thread.sleep(1000); // 等待1秒后再次轮询
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            String finalReply = aiReply.length() > 0 ? aiReply.toString() : "AI回复超时或为空";
            System.out.println("FINAL！！！！——————————\n" + finalReply);

            // 保存AI回复
            Record aiRecord = new Record(sid, uid, false, finalReply, nextSeq + 1, MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
            aiRecord = recordRepository.save(aiRecord);
            return aiRecord.toVO();

        } catch (Exception e) {
            log.error("调用AI服务失败", e);
            // 保存错误消息
            String errMsg = "AI回复失败: " + e.getMessage();
            Record errRecord = new Record(sid, uid, false, errMsg, nextSeq + 1, MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
            errRecord = recordRepository.save(errRecord);
            return errRecord.toVO();
        }
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
        boolean foundFinishStart = false;
        StringBuilder fullDelta = new StringBuilder();

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

                        // 更新foundFinishStart状态
                        if (!foundFinishStart && fullDelta.toString().contains("<finish>")) {
                            foundFinishStart = true;
                            System.out.println("发现开始标签，开始处理内容");
                        }

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
                                       StringBuilder aiReply, StringBuilder fullDelta, boolean foundFinishStart) throws Exception {

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
                                     StringBuilder aiReply, StringBuilder fullDelta, boolean foundFinishStart) throws Exception {

        String eventSessionId = (String) event.get("session_id");
        String userId = (String) event.get("user_id");
        String eventType = (String) event.get("event");
        String tool = (String) event.get("tool");

        // 打印事件详情到控制台
        System.out.println(String.format("事件详情 - session_id: %s, user_id: %s, event: %s, tool: %s",
            eventSessionId, userId, eventType, tool));

        // 只处理当前会话的llm_tool.chunk事件
        if (!sessionId.equals(eventSessionId) || !"llm_tool.chunk".equals(eventType)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        var payload = (java.util.Map<String, Object>) event.get("payload");
        if (payload != null && payload.get("delta") != null) {
            String delta = payload.get("delta").toString();

            // 打印delta内容
            System.out.println("Delta内容: " + delta);

            if (!delta.isEmpty()) {
                fullDelta.append(delta);

                // 检查是否包含finish标签
                String fullDeltaStr = fullDelta.toString();
                if (fullDeltaStr.contains("</finish>")) {
                    log.info("检测到</finish>标签，AI回复生成完成");
                    
                    // 发送最后的内容片段（如果有的话）
                    String finalContent = extractContentFromDelta(fullDeltaStr);
                    if (!finalContent.isEmpty() && finalContent.length() > aiReply.length()) {
                        String newContent = finalContent.substring(aiReply.length());
                        if (!newContent.isEmpty()) {
                            aiReply.append(newContent);
                            // 实时发送最后的内容片段到前端
                            sendDeltaToFrontend(emitter, newContent);
                        }
                    }
                    
                    return true; // 生成完成
                }

                // 实时处理和发送内容，无论是否找到开始标签
                if (foundFinishStart || fullDeltaStr.contains("<finish>")) {
                    String extractedContent = extractContentFromDelta(fullDeltaStr);
                    if (!extractedContent.isEmpty() && extractedContent.length() > aiReply.length()) {
                        // 计算新增的内容
                        String newContent = extractedContent.substring(aiReply.length());
                        if (!newContent.isEmpty()) {
                            aiReply.append(newContent);
                            // 实时发送新增内容到前端
                            sendDeltaToFrontend(emitter, newContent);
                            System.out.println("实时发送内容片段: " + newContent);
                        }
                    }
                } else {
                    // 如果还没找到开始标签，但有delta内容，也尝试直接发送
                    // 这确保不会错过任何有用的内容
                    if (delta.trim().length() > 0 && !delta.contains("<") && !delta.contains(">")) {
                        // 如果delta不包含标签，可能是纯文本内容，直接发送
                        aiReply.append(delta);
                        sendDeltaToFrontend(emitter, delta);
                        System.out.println("直接发送delta内容: " + delta);
                    }
                }
            }
        }

        return false;
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
     * 从完整的delta字符串中提取finish标签内的内容
     */
    private String extractContentFromDelta(String deltaStr) {
        try {
            int startIndex = deltaStr.indexOf("<finish>");
            if (startIndex == -1) {
                return "";
            }

            int endIndex = deltaStr.indexOf("</finish>");
            if (endIndex == -1) {
                // 还没有结束标签，返回开始标签后的所有内容
                String content = deltaStr.substring(startIndex + "<finish>".length());
                // 查找answer: 后的内容
                int answerIndex = content.indexOf("answer:");
                if (answerIndex != -1) {
                    return content.substring(answerIndex + "answer:".length()).trim();
                }
                return content.trim();
            } else {
                // 有完整的finish标签，提取中间的内容
                String content = deltaStr.substring(startIndex + "<finish>".length(), endIndex);
                // 查找answer: 后的内容
                int answerIndex = content.indexOf("answer:");
                if (answerIndex != -1) {
                    return content.substring(answerIndex + "answer:".length()).trim();
                }
                return content.trim();
            }
        } catch (Exception e) {
            log.warn("提取delta内容失败: " + deltaStr, e);
            return "";
        }
    }
}
