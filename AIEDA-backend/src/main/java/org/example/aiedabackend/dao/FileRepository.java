package org.example.aiedabackend.dao;

import org.example.aiedabackend.po.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, String> {
    
    /**
     * 根据用户ID查询文件列表
     */
    List<File> findByUid(Integer uid);
    
    /**
     * 根据用户ID和会话ID查询文件列表
     */
    List<File> findByUidAndSid(Integer uid, Integer sid);
    
    /**
     * 根据会话ID查询文件列表
     */
    List<File> findBySid(Integer sid);
    
    /**
     * 根据文件ID查询文件
     */
    File findByFileId(String fileId);
}
