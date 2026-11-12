package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 答题计数投影（条件聚合单行结果）。
 */
@Data
public class AnswerCountVO {

    private Integer totalCount;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer ungradedCount;
}
