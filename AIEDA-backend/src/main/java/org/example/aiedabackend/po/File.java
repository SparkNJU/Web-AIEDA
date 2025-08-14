package org.example.aiedabackend.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.aiedabackend.vo.FileVO;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "files")
public class File {

    @Id
    @Column(name = "file_id", length = 36)
    private String fileId;

    @Column(nullable = false)
    private Integer uid;

    @Column(nullable = false)
    private Integer sid;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "saved_name", nullable = false, length = 255)
    private String savedName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    public File(String fileId, Integer uid, Integer sid, String originalName, 
                String savedName, String filePath, Long fileSize, String fileType, 
                LocalDateTime uploadTime) {
        this.fileId = fileId;
        this.uid = uid;
        this.sid = sid;
        this.originalName = originalName;
        this.savedName = savedName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.uploadTime = uploadTime;
    }

    public FileVO toVO() {
        FileVO fileVO = new FileVO();
        fileVO.setFileId(this.fileId);
        fileVO.setOriginalName(this.originalName);
        fileVO.setSavedName(this.savedName);
        fileVO.setFilePath(this.filePath);
        fileVO.setFileSize(this.fileSize);
        fileVO.setFileType(this.fileType);
        fileVO.setUploadTime(this.uploadTime);
        fileVO.setUserId(this.uid.toString());
        fileVO.setSessionId(this.sid.toString());
        fileVO.setDownloadUrl("/api/files/download/" + this.fileId);
        return fileVO;
    }
}
