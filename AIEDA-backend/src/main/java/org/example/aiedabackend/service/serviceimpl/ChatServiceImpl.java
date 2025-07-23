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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
            boolean deltaFinishStart = false;
            boolean deltaFinishEnd = false;

            while (System.currentTimeMillis() - startTime < timeout) {
                deltaFinishStart = false;
                deltaFinishEnd = false;
                try {
                    // 获取最新事件
                    String eventsUrlWithParams = eventsUrl + "?session_id=" + sessionId + "&limit=5000";
                    var eventsResponse = restTemplate.getForObject(eventsUrlWithParams, java.util.Map.class);

                    if (eventsResponse != null && "success".equals(eventsResponse.get("status"))) {
                        //System.out.println("eventsResponse: " + eventsResponse.toString());
                        @SuppressWarnings("unchecked")
                        java.util.List<java.util.Map<String, Object>> events =
                            (java.util.List<java.util.Map<String, Object>>) eventsResponse.get("events");

                        if (events != null) {
                            for (var event : events) {
                                @SuppressWarnings("unchecked")
                                var data = (java.util.Map<String, Object>) event.get("data");
                                if (data != null) {
                                    @SuppressWarnings("unchecked")
                                    var payload = (java.util.Map<String, Object>) data.get("payload");
                                    if (payload != null && payload.get("delta") != null) {
                                        String delta = payload.get("delta").toString();
                                        if (!delta.isEmpty()) {
                                            aiReply.append(delta);
                                            if(delta.startsWith("<finish>")){
                                                deltaFinishStart = true;
                                            }
                                            if(delta.contains("</finish>")){
                                                deltaFinishEnd = true;
                                            }
                                            if(deltaFinishStart && deltaFinishEnd){
                                                System.out.println("AI回复结束标志已检测到，停止轮询");
                                                break;
                                            }
                                            System.out.println("AI回复片段: {}"+ delta);
                                        }
                                    }

                                    // 检查是否有最终结果
                                    @SuppressWarnings("unchecked")
                                            //TODO: 这里的类型转换可能需要根据实际情况调整
                                    var result = (java.util.Map<String, Object>) data.get("result");
                                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                                        // 处理完成，跳出循环
//                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // 如果有回复内容，暂停轮询
                    if (aiReply.length() > 0) {
                        // 继续轮询一小段时间以确保获取完整回复
                        Thread.sleep(500);
                        break;
                    }

                    Thread.sleep(1000); // 等待1秒后再次轮询
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            String finalReply = aiReply.length() > 0 ? aiReply.toString() : "AI回复超时或为空";
            System.out.println("FINAL" + finalReply);

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
}
