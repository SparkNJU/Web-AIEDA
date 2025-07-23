package org.example.aiedabackend.dao;

import org.example.aiedabackend.po.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, Integer> {
    // 根据会话ID查询所有记录并按顺序排列
    List<Record> findBySidOrderBySequenceAsc(Integer sid);

    /**
     * 删除指定会话的所有记录
     */
    void deleteBySid(Integer sid);
}
