package org.example.aiedabackend.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.aiedabackend.vo.SessionVO;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sid;

    @Column(nullable = false)
    private Integer uid;

    @Column(length = 50, nullable = false)
    private String title;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    public Session(Integer uid, String title, LocalDateTime createTime, LocalDateTime updateTime) {
        this.uid = uid;
        this.title = title;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public SessionVO toVO() {
        SessionVO sessionVO = new SessionVO();
        sessionVO.setSid(this.sid);
        sessionVO.setTitle(this.title);
        sessionVO.setCreateTime(this.createTime);
        sessionVO.setUpdateTime(this.updateTime);
        return sessionVO;
    }
}
