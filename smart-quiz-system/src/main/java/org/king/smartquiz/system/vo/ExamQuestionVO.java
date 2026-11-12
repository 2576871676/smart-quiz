package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 考试作答页题目（不含答案与解析，防提前泄题）。
 */
@Data
public class ExamQuestionVO {

    private Long questionId;
    private Integer sort;
    private Integer score;
    private Integer type;
    private Integer difficulty;
    private String title;
    private String options;
}
