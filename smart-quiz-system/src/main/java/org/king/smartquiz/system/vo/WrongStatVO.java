package org.king.smartquiz.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 错题统计：总量、掌握率、科目分布、薄弱知识点 TOP。
 */
@Data
public class WrongStatVO {

    private Integer totalCount;
    private Integer masteredCount;
    /** 掌握率（百分比，保留 1 位小数） */
    private Double masteryRate;
    private List<SubjectStat> subjectStats;
    private List<WeakPoint> weakPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectStat {
        private String subject;
        private Integer count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakPoint {
        private String knowledgePoint;
        /** 累计错误次数 */
        private Integer wrongCount;
        /** 涉及题目数 */
        private Integer questionCount;
    }
}
