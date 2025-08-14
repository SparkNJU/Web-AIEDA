package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FilePreviewVO {
    private String fileId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String previewContent;
    private Boolean isTruncated;
}
