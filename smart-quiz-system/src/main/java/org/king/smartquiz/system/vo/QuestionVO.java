package org.king.smartquiz.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目返回对象。
 */
@Data
public class QuestionVO {

    private Long id;
    private Long userId;
    private String subject;
    private String knowledgePoint;
    private Integer type;
    private Integer difficulty;
    private String title;
    private String options;
    private String answer;
    private String analysis;
    private Integer score;
    private Integer source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
