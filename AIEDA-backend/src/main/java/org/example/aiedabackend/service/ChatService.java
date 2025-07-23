package org.example.aiedabackend.service;

import org.example.aiedabackend.vo.RecordVO;
import org.example.aiedabackend.vo.SessionVO;

import java.util.List;

public interface ChatService {
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
     * 发送消息，并接收AI回复
     */
    RecordVO sendMessage(Integer uid, Integer sid, String content);

    /**
     * 更新会话标题
     */
    boolean updateSessionTitle(Integer uid, Integer sid, String title);

    /**
     * 删除会话及其所有记录
     */
    boolean deleteSession(Integer uid, Integer sid);
}
