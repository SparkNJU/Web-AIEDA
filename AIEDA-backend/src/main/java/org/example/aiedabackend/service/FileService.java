package org.example.aiedabackend.service;

import org.example.aiedabackend.vo.FileListResponseVO;
import org.example.aiedabackend.vo.FilePreviewVO;
import org.example.aiedabackend.vo.FileVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    
    /**
     * 上传文件
     */
    FileVO uploadFile(MultipartFile file, String uid, String sid, String metadata);
    
    /**
     * 下载文件
     */
    Resource downloadFile(String fid);
    
    /**
     * 预览文件
     */
    FilePreviewVO previewFile(String fid);
    
    /**
     * 获取用户文件列表
     */
    FileListResponseVO getUserFiles(String uid, String sid);
    
    /**
     * 根据文件ID获取文件信息
     */
    FileVO getFileInfo(String fid);
    
    /**
     * 获取文件下载URL
     */
    String getDownloadUrl(String fid);
    
    /**
     * 获取文件预览URL
     */
    String getPreviewUrl(String fid);
    
    /**
     * 删除文件
     */
    boolean deleteFile(String fid);
    
    /**
     * 从大模型服务获取文件结构列表
     */
    FileListResponseVO getFileStructureFromLLM(String uid, String sid);
    
    /**
     * 通过本地路径访问文件内容
     */
    Resource getFileByLocalPath(String uid, String sid, String filename);
}
