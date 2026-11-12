package org.king.smartquiz.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 试卷内单题条目。
 */
@Data
public class ExamPaperItemDTO {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /** 该题分值，缺省 5 */
    private Integer score;
}
