package org.king.smartquiz.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开考返回的考试会话：记录 ID + 限时 + 题目（无答案）。
 */
@Data
public class ExamSessionVO {

    private Long recordId;
    private Long paperId;
    private String title;
    private String subject;
    /** 考试时长（分钟） */
    private Integer duration;
    private LocalDateTime startTime;
    private List<ExamQuestionVO> questions;
}
