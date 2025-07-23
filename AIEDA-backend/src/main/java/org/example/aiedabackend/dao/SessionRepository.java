package org.example.aiedabackend.dao;

import org.example.aiedabackend.po.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {
    // 根据用户ID查询会话列表
    List<Session> findByUid(Integer uid);
}
