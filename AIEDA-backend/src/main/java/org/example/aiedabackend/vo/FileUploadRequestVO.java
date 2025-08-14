package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileUploadRequestVO {
    private String userId;
    private String sessionId;
    private String metadata;
}
