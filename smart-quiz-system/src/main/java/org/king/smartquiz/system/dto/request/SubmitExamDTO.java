package org.king.smartquiz.system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交试卷作答：考试会话 ID + 各题答案。
 */
@Data
public class SubmitExamDTO {

    @NotNull(message = "考试记录ID不能为空")
    private Long recordId;

    @NotEmpty(message = "答卷不能为空")
    @Valid
    private List<AnswerItemDTO> answers;
}
