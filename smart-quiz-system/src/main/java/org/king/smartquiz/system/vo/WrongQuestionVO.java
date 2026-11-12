package org.king.smartquiz.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本条目（含题目详情，便于复习）。
 */
@Data
public class WrongQuestionVO {

    private Long id;
    private Long questionId;

    // 题目信息（联表补充）
    private String subject;
    private String knowledgePoint;
    private Integer type;
    private Integer difficulty;
    private String title;
    private String options;
    private String answer;
    private String analysis;

    // 错题信息
    private Integer wrongCount;
    private Integer mastered;
    private LocalDateTime lastWrongTime;
}
