package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FileVO {
    private String fileId;
    private String originalName;
    private String savedName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadTime;
    private String downloadUrl;
    private String userId;
    private String sessionId;
}
