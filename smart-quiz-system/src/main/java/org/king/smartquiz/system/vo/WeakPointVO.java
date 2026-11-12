package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 薄弱知识点（按答错次数排序）。
 */
@Data
public class WeakPointVO {

    private String knowledgePoint;
    /** 累计答错次数 */
    private Integer wrongCount;
}
