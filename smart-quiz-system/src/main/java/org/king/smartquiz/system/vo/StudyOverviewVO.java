package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 学习总览。
 */
@Data
public class StudyOverviewVO {

    private Integer totalAnswered;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer ungradedCount;
    /** 总正确率（百分比，1 位小数） */
    private Double accuracy;

    /** 错题本未移除条目数 */
    private Integer wrongBookCount;
    /** 错题本已掌握数 */
    private Integer masteredCount;

    /** 最近统计天数 */
    private Integer recentDays;
    /** 最近 N 天答题数 */
    private Integer recentAnswered;
}
