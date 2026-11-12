package org.king.smartquiz.system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 保存试卷（新建/更新共用）：基本信息 + 题目清单（顺序即题序）。
 */
@Data
public class SaveExamPaperDTO {

    @NotBlank(message = "试卷标题不能为空")
    private String title;

    private String subject;

    private String description;

    /** 考试时长（分钟），缺省 60 */
    private Integer duration;

    /** 状态：0-草稿，1-已发布；缺省 0 */
    private Integer status;

    @NotEmpty(message = "试卷至少包含一道题")
    @Valid
    private List<ExamPaperItemDTO> items;
}
