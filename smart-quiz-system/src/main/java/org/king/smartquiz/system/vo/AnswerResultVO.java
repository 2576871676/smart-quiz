package org.king.smartquiz.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 答卷批改汇总结果。
 */
@Data
public class AnswerResultVO {

    /** 总得分 */
    private Integer obtainedScore;
    /** 试卷满分 */
    private Integer totalScore;
    private Integer correctCount;
    private Integer wrongCount;
    /** 未判分题数（主观题 AI 批改失败） */
    private Integer ungradedCount;
    private Integer totalCount;
    /** 正确率（百分比，保留 1 位小数） */
    private Double accuracy;
    private List<AnswerDetailVO> details;
}
