package org.king.smartquiz.system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 提交答卷入参（练习模式：直接对一组题目作答，无需试卷）。
 */
@Data
public class SubmitAnswerDTO {

    @Valid
    @NotEmpty(message = "答题内容不能为空")
    private List<AnswerItemDTO> answers;
}
