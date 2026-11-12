package org.king.smartquiz.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 多维度正确率统计。
 */
@Data
public class AccuracyStatVO {

    private List<DimensionStatVO> byType;
    private List<DimensionStatVO> byDifficulty;
    private List<DimensionStatVO> bySubject;
}
