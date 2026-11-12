package org.king.smartquiz.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答题记录（历史）返回对象。
 */
@Data
public class AnswerRecordVO {

    private Long id;
    private Long questionId;
    private String title;
    private String subject;
    private Integer type;
    private String userAnswer;
    private Integer isCorrect;
    private Integer score;
    private String aiComment;
    private Integer duration;
    private LocalDateTime createTime;
}
