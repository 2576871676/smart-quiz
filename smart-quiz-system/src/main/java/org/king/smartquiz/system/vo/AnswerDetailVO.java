package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 单题批改结果明细。
 */
@Data
public class AnswerDetailVO {

    private Long questionId;
    private String title;
    private Integer type;
    private Integer difficulty;
    private String options;
    private String userAnswer;
    private String referenceAnswer;
    private String analysis;

    /** 是否正确：1-对，0-错，null-未判分（主观题 AI 批改失败时） */
    private Integer isCorrect;
    /** 得分 */
    private Integer score;
    /** 满分 */
    private Integer fullScore;
    /** AI 评语（主观题） */
    private String aiComment;
}
