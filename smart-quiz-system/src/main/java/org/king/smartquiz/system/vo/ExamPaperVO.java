package org.king.smartquiz.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷列表项。
 */
@Data
public class ExamPaperVO {

    private Long id;
    private String title;
    private String subject;
    private String description;
    private Integer totalScore;
    private Integer questionCount;
    /** 考试时长（分钟） */
    private Integer duration;
    /** 状态：0-草稿，1-已发布 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
