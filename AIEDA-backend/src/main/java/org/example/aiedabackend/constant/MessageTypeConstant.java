package org.example.aiedabackend.constant;

/**
 * 消息类型常量类，定义消息类型的数值与含义映射
 */
public class MessageTypeConstant {
    /**
     * 用户消息
     */
    public static final int USER = 0;

    /**
     * LLM生成的消息（不可修改）
     */
    public static final int LLM_GENERATION = 1;

    /**
     * LLM发送的需要用户确认的消息（可修改）
     */
    public static final int LLM_TO_CONFIRM = 2;

    /**
     * 工具执行结果（不可修改）
     */
    public static final int TOOL_EXECUTION_RESULT = 3;

    /**
     * 私有构造函数防止实例化
     */
    private MessageTypeConstant() {
    }
}
