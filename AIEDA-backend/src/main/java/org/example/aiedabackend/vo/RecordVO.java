package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RecordVO {
    /**
     * 记录ID
     */
    private Integer rid;

    /**
     * 会话ID，关联到对应的会话
     */
    private Integer sid;

    /**
     * 消息方向：true表示用户发送的消息，false表示AI回复的消息
     */
    private Boolean direction;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息顺序，用于确保对话展示的正确顺序
     */
    private Integer sequence;

    /**
     * 消息类型
     * USER: 用户发送的消息
     * LLM_GENERATION: LLM生成的消息（不可修改）
     * LLM_TO_CONFIRM: LLM发送的需要用户确认的消息（可修改）
     * TOOL_EXECUTION_RESULT: 工具执行结果（不可修改）
     */
    private Integer type;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;
}
