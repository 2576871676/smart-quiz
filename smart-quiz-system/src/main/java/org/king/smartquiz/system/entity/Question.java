package org.king.smartquiz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目实体，对应表 question。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question")
public class Question extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建用户ID */
    private Long userId;

    /** 科目 */
    private String subject;

    /** 知识点 */
    private String knowledgePoint;

    /** 题型：1-单选，2-多选，3-填空，4-简答 */
    private Integer type;

    /** 难度：1-简单，2-中等，3-困难 */
    private Integer difficulty;

    /** 题干 */
    private String title;

    /** 选项（JSON 格式，选择题用） */
    private String options;

    /** 参考答案 */
    private String answer;

    /** 答案解析 */
    private String analysis;

    /** 分值 */
    private Integer score;

    /** 来源：1-AI生成，2-手动录入 */
    private Integer source;
}
