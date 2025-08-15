package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequestVO {
    private Integer uid;
    private String content;
    private Integer sid; // 会话ID
    private List<String> fileReferences; // 引用的文件ID列表，可选
    private String agentType; // Agent类型：orchestrator 或 dynamic，可选，默认为orchestrator
    private String inputType; // 输入类型：question、config、intervention、delete，可选，默认为question
    
    // LLM自定义配置字段（仅在inputType为config时使用）
    private String apiKey; // 自定义API Key
    private String baseUrl; // 自定义Base URL
    private String model; // 自定义模型
}
