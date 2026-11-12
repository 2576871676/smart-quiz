package org.king.smartquiz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试记录（会话）实体，对应表 exam_record。
 * 该表无 deleted / create_time / update_time 列，故不继承 BaseEntity；
 * 每题作答明细落 answer_record（paper_id 关联本试卷）。
 */
@Data
@TableName("exam_record")
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long paperId;

    /** 得分 */
    private Integer totalScore;

    private Integer correctCount;

    private Integer wrongCount;

    /** 实际用时（秒） */
    private Integer duration;

    /** 状态：0-进行中，1-已提交 */
    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime submitTime;
}
