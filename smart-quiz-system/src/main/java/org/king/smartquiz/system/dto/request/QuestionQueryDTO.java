package org.king.smartquiz.system.dto.request;

import lombok.Data;

/**
 * 题目分页查询入参。GET 请求以查询参数绑定。
 */
@Data
public class QuestionQueryDTO {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String subject;
    private Integer type;
    private Integer difficulty;
    private String knowledgePoint;
    /** 题干关键词模糊搜索 */
    private String keyword;
    /** 仅看自己创建的题目 */
    private Boolean onlyMine = false;
}
