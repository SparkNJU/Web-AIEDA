package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequestVO {
    private Integer uid;
    private String content;
    private Integer sid; // 会话ID
}
