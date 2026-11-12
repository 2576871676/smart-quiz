package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 试卷内题目（管理端详情，含答案与解析）。
 */
@Data
public class ExamPaperQuestionVO {

    private Long questionId;
    private Integer sort;
    private Integer score;

    // 题目信息
    private String title;
    private Integer type;
    private Integer difficulty;
    private String options;
    private String answer;
    private String analysis;
}
