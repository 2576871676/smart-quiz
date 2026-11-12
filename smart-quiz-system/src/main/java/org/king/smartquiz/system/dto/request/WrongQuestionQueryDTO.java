package org.king.smartquiz.system.dto.request;

import lombok.Data;

/**
 * 错题本分页查询入参（GET 查询参数绑定）。
 */
@Data
public class WrongQuestionQueryDTO {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /** 按科目筛选 */
    private String subject;
    /** 按知识点筛选 */
    private String knowledgePoint;
    /** 按掌握状态筛选：true 已掌握 / false 未掌握 / null 全部 */
    private Boolean mastered;
    /** 排序字段：wrongCount-按错误次数，其余按最近错误时间 */
    private String sortBy;
}
