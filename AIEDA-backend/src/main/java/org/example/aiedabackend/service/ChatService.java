package org.example.aiedabackend.service;

import org.example.aiedabackend.vo.RecordVO;
import org.example.aiedabackend.vo.SessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {
    /**
     * 为指定会话创建独立的SSE连接
     */
    SseEmitter createSessionSSE(Integer uid, Integer sid);

    /**
     * 获取用户所有会话列表
     */
    List<SessionVO> getSessions(Integer uid);

    /**
     * 创建新的会话
     */
    SessionVO createSession(Integer uid, String title);

    /**
     * 获取指定会话的所有对话记录
     */
    List<RecordVO> getRecords(Integer sid);

    /**
     * 发送消息，并通过SSE流式返回AI回复
     */
    SseEmitter sendMessageSSE(Integer uid, Integer sid, String content);

    /**
     * 发送消息，并通过SSE流式返回AI回复（支持metadata）
     */
    SseEmitter sendMessageSSE(Integer uid, Integer sid, String content, java.util.Map<String, Object> metadata);

    /**
     * 发送带文件引用的消息，并通过SSE流式返回AI回复
     */
    SseEmitter sendMessageWithFilesSSE(Integer uid, Integer sid, String content, List<String> fileReferences);

    /**
     * 发送带文件引用的消息，并通过SSE流式返回AI回复（支持metadata）
     */
    SseEmitter sendMessageWithFilesSSE(Integer uid, Integer sid, String content, List<String> fileReferences, java.util.Map<String, Object> metadata);

    /**
     * 发送配置消息（不等待流式回复，仅发送配置）
     */
    SseEmitter sendConfigMessage(Integer uid, Integer sid, String agentType);

    /**
     * 发送自定义配置消息（支持用户自定义的LLM配置）
     */
    SseEmitter sendConfigMessage(Integer uid, Integer sid, String agentType, String apiKey, String baseUrl, String model);

    /**
     * 更新会话标题
     */
    boolean updateSessionTitle(Integer uid, Integer sid, String title);

    /**
     * 删除会话及其所有记录
     */
    boolean deleteSession(Integer uid, Integer sid);

    /**
     * 发送非流式消息（用于config、delete、intervention类型）
     * 不建立SSE连接，直接发送到大模型
     */
    boolean sendMessageInput(Integer uid, Integer sid, String content, java.util.Map<String, Object> metadata, String inputType);

    /**
     * 停止指定会话的SSE连接超时监控
     * 用于硬干预时暂停超时计时
     */
    boolean stopSessionTimeout(Integer sid);

    /**
     * 重启指定会话的SSE连接超时监控
     * 用于软干预后恢复超时计时
     */
    boolean restartSessionTimeout(Integer sid);
}
