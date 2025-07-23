package org.example.aiedabackend.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.aiedabackend.vo.RecordVO;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "records")
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer rid;

    @Column(nullable = false)
    private Integer sid;

    @Column(nullable = false)
    private Integer uid;

    @Column(nullable = false)
    private Boolean direction;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private Integer type;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    public Record(Integer sid, Integer uid, Boolean direction, String content, Integer sequence, Integer type, LocalDateTime createTime) {
        this.sid = sid;
        this.uid = uid;
        this.direction = direction;
        this.content = content;
        this.sequence = sequence;
        this.type = type;
        this.createTime = createTime;
    }

    public RecordVO toVO() {
        RecordVO recordVO = new RecordVO();
        recordVO.setRid(this.rid);
        recordVO.setSid(this.sid);
        recordVO.setDirection(this.direction);
        recordVO.setContent(this.content);
        recordVO.setSequence(this.sequence);
        recordVO.setType(this.type);
        recordVO.setCreateTime(this.createTime);
        return recordVO;
    }
}
