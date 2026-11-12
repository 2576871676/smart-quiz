package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 单维度统计项（题型 / 难度 / 科目通用）。
 */
@Data
public class DimensionStatVO {

    /** 维度名称，如「单选题」「中等」「Java」 */
    private String name;
    /** 答题总数 */
    private Integer total;
    /** 答对数 */
    private Integer correct;
    /** 正确率（百分比，保留 1 位小数） */
    private Double accuracy;
}
