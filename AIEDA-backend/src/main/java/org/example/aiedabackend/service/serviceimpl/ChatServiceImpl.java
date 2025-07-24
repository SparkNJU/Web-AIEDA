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
import org.springframework.scheduling.annotation.Async;

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
            StringBuilder aiReply = new StringBuilder();
            StringBuilder fullDelta = new StringBuilder(); // 用于累积完整的delta内容
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
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"type\":\"error\",\"message\":\"提交用户输入失败\"}"));
                    emitter.complete();
                    return;
                }

                // 发送开始信号
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("{\"type\":\"start\",\"message\":\"AI正在思考...\"}"));

                // 2. 轮询获取AI回复（使用轮询而不是SSE连接，因为后端SSE可能有过滤问题）
                String sessionId = sid.toString();
                long startTime = System.currentTimeMillis();
                long timeout = 60000; // 60秒超时
                boolean generationFinished = false;
                java.util.Set<Integer> processedEventIds = new java.util.HashSet<>();
                boolean foundFinishStart = false;
                boolean foundFinishEnd = false;

                while (System.currentTimeMillis() - startTime < timeout && !generationFinished) {
                    try {
                        // 获取最新事件
                        String eventsUrlWithParams = eventsUrl + "?session_id=" + sessionId + "&limit=5000";
                        var eventsResponse = restTemplate.getForObject(eventsUrlWithParams, java.util.Map.class);

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

                                    // 检查是否是llm_tool.chunk事件
                                    String eventType = (String) event.get("event");
                                    if (!"llm_tool.chunk".equals(eventType)) {
                                        continue;
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
                                                fullDelta.append(delta);

                                                // 检查是否包含finish标签
                                                String fullDeltaStr = fullDelta.toString();
                                                if (fullDeltaStr.contains("<finish>")) {
                                                    foundFinishStart = true;
                                                }
                                                if (fullDeltaStr.contains("</finish>")) {
                                                    foundFinishEnd = true;
                                                    generationFinished = true;
                                                }

                                                // 如果找到了开始标签，提取并发送内容
                                                if (foundFinishStart) {
                                                    String extractedContent = extractContentFromDelta(fullDeltaStr);
                                                    if (!extractedContent.isEmpty() && !extractedContent.equals(aiReply.toString())) {
                                                        // 只发送新增的部分
                                                        String newContent = extractedContent.substring(aiReply.length());
                                                        if (!newContent.isEmpty()) {
                                                            aiReply.append(newContent);

                                                            // 发送流式数据到前端
                                                            String deltaJson = String.format(
                                                                "{\"type\":\"delta\",\"content\":\"%s\"}",
                                                                newContent.replace("\"", "\\\"")
                                                                          .replace("\n", "\\n")
                                                                          .replace("\r", "\\r")
                                                                          .replace("\t", "\\t"));
                                                            emitter.send(SseEmitter.event()
                                                                .name("message")
                                                                .data(deltaJson));
                                                        }
                                                    }
                                                }
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

                        Thread.sleep(500); // 等待0.5秒后再次轮询，提高响应速度

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.warn("轮询事件时发生错误", e);
                    }
                }

                // 保存完整的AI回复
                String finalReply = aiReply.length() > 0 ? aiReply.toString() : "AI回复为空";
                Record aiRecord = new Record(sid, uid, false, finalReply, nextSeq + 1,
                    MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
                recordRepository.save(aiRecord);

                // 发送完成信号
                String completeJson = String.format(
                    "{\"type\":\"complete\",\"message\":\"回复完成\",\"recordId\":%d}",
                    aiRecord.getRid());
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(completeJson));

            } catch (Exception e) {
                log.error("SSE流式处理失败", e);
                try {
                    // 保存错误消息
                    String errMsg = "AI回复失败: " + e.getMessage();
                    Record errRecord = new Record(sid, uid, false, errMsg, nextSeq + 1,
                        MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
                    recordRepository.save(errRecord);

                    // 发送错误信息
                    String errorJson = String.format(
                        "{\"type\":\"error\",\"message\":\"%s\"}",
                        e.getMessage().replace("\"", "\\\""));
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errorJson));
                } catch (Exception sendError) {
                    log.error("发送错误消息失败", sendError);
                }
            } finally {
                emitter.complete();
            }
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
