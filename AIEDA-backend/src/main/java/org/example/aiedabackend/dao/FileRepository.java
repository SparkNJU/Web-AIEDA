package org.example.aiedabackend.dao;

import org.example.aiedabackend.po.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    
    /**
     * 根据用户ID和会话ID查询未关联记录的文件列表（rid为null）
     */
    List<File> findByUidAndSidAndRidIsNull(Integer uid, Integer sid);
    
    /**
     * 根据记录ID查询关联的文件列表
     */
    List<File> findByRid(Integer rid);
    
    /**
     * 根据文件ID列表更新关联的记录ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE File f SET f.rid = :rid WHERE f.fileId IN :fileIds")
    void updateRidByFileIds(@Param("fileIds") List<String> fileIds, @Param("rid") Integer rid);
}
