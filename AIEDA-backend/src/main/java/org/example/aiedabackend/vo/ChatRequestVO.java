package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequestVO {
    private Integer uid;
    private String content;
    private Integer sid; // 会话ID
    private List<String> fileReferences; // 引用的文件ID列表，可选
    private String inputType; // 输入类型：question、config、intervention、delete，可选，默认为question
    
    // 元数据字段（用于传递所有可选配置信息）
    private Map<String, Object> metadata;
}
