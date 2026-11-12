package org.king.smartquiz.system.dto.request;

import lombok.Data;

/**
 * 试卷分页查询条件。
 */
@Data
public class ExamPaperQueryDTO {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String subject;

    /** 状态：0-草稿，1-已发布；空为全部 */
    private Integer status;

    /** 标题关键字 */
    private String keyword;

    /** 仅看我创建的 */
    private Boolean onlyMine = false;
}
