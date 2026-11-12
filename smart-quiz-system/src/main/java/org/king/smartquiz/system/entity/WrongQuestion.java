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
 * 错题本实体，对应表 wrong_question（uk_user_question 唯一约束 user_id+question_id）。
 * 注意：该表无 deleted 列，故不继承 BaseEntity。
 */
@Data
@TableName("wrong_question")
public class WrongQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** 累计错误次数 */
    private Integer wrongCount;

    private LocalDateTime lastWrongTime;

    /** 是否已掌握：0-未掌握，1-已掌握 */
    private Integer mastered;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
