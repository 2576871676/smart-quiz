package org.king.smartquiz.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单题作答项。
 */
@Data
public class AnswerItemDTO {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /** 用户答案；未作答可为空 */
    private String userAnswer;

    /** 本题用时（秒），可选 */
    private Integer duration;
}
