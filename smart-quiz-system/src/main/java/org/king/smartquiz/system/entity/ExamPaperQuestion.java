package org.king.smartquiz.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 试卷-题目关系实体，对应表 exam_paper_question。
 * 该表无 deleted / update_time 列，故不继承 BaseEntity。
 */
@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long questionId;

    /** 试卷内题序 */
    private Integer sort;

    /** 该题在试卷中的分值 */
    private Integer score;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
