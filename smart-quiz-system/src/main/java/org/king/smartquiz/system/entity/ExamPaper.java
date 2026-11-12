package org.king.smartquiz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 试卷实体，对应表 exam_paper（含 deleted，继承 BaseEntity 获得逻辑删除与时间填充）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_paper")
public class ExamPaper extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建者（教师/学生）ID */
    private Long userId;

    private String title;

    private String subject;

    private String description;

    /** 总分（由题目分值汇总） */
    private Integer totalScore;

    /** 题目数量 */
    private Integer questionCount;

    /** 考试时长（分钟） */
    private Integer duration;

    /** 状态：0-草稿，1-已发布 */
    private Integer status;
}
