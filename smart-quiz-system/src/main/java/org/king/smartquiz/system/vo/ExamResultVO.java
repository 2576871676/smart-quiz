package org.king.smartquiz.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 交卷批改结果（试卷模式）。
 */
@Data
public class ExamResultVO {

    private Long recordId;
    private Integer obtainedScore;
    private Integer totalScore;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer ungradedCount;
    private Integer totalCount;
    /** 正确率（百分比，1 位小数） */
    private Double accuracy;
    /** 实际用时（秒） */
    private Integer duration;
    private List<AnswerDetailVO> details;
}
