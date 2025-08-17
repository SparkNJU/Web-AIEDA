package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FileStructureResponseVO {
    /**
     * 根目录文件节点列表
     */
    private List<FileNodeVO> fileTree;
    
    /**
     * 总文件数量
     */
    private Integer totalFileCount;
    
    /**
     * 总文件夹数量
     */
    private Integer totalFolderCount;
}
