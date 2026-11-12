package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 答题趋势单日数据点。
 */
@Data
public class TrendPointVO {

    /** 日期 yyyy-MM-dd */
    private String date;
    private Integer total;
    private Integer correct;
    private Double accuracy;
}
