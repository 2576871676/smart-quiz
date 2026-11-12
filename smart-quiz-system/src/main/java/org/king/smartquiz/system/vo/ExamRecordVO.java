package org.king.smartquiz.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的考试记录列表项。
 */
@Data
public class ExamRecordVO {

    private Long id;
    private Long paperId;
    private String paperTitle;
    private String subject;
    /** 得分 */
    private Integer totalScore;
    private Integer correctCount;
    private Integer wrongCount;
    /** 用时（秒） */
    private Integer duration;
    /** 状态：0-进行中，1-已提交 */
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
