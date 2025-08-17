package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FileNodeVO {
    /**
     * 节点名称（文件名或文件夹名）
     */
    private String name;
    
    /**
     * 节点类型：file（文件）或 folder（文件夹）
     */
    private String type;
    
    /**
     * 节点路径（相对于工作空间根目录）
     */
    private String path;
    
    /**
     * 文件ID（仅当type为file时有效）
     */
    private String fileId;
    
    /**
     * 下载URL（仅当type为file时有效）
     */
    private String downloadUrl;
    
    /**
     * 文件大小（仅当type为file时有效）
     */
    private Long fileSize;
    
    /**
     * 文件类型（仅当type为file时有效）
     */
    private String fileType;
    
    /**
     * 子节点（仅当type为folder时有效）
     */
    private List<FileNodeVO> children;
    
    /**
     * 是否展开（用于前端显示）
     */
    private Boolean expanded;
    
    /**
     * 创建文件节点
     */
    public static FileNodeVO createFileNode(String name, String path, String fileId, String downloadUrl) {
        FileNodeVO node = new FileNodeVO();
        node.setName(name);
        node.setType("file");
        node.setPath(path);
        node.setFileId(fileId);
        node.setDownloadUrl(downloadUrl);
        node.setExpanded(false);
        return node;
    }
    
    /**
     * 创建文件夹节点
     */
    public static FileNodeVO createFolderNode(String name, String path, List<FileNodeVO> children) {
        FileNodeVO node = new FileNodeVO();
        node.setName(name);
        node.setType("folder");
        node.setPath(path);
        node.setChildren(children);
        node.setExpanded(false);
        return node;
    }
}
