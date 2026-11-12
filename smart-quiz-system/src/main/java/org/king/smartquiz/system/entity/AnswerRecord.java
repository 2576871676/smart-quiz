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
 * 答题记录实体，对应表 answer_record。
 * 注意：该表无 deleted / update_time 列，故不继承 BaseEntity（避免逻辑删除与更新时间填充误用）。
 */
@Data
@TableName("answer_record")
public class AnswerRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 试卷ID，单题/练习作答为空 */
    private Long paperId;

    private Long questionId;

    private String userAnswer;

    /** 是否正确：0-错，1-对，null-未判分 */
    private Integer isCorrect;

    private Integer score;

    private String aiComment;

    /** 答题用时（秒） */
    private Integer duration;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
