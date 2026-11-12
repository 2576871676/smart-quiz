package org.king.smartquiz.system.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 出题入参。
 */
@Data
public class AiGenerateDTO {

    @NotBlank(message = "科目不能为空")
    @Size(max = 50, message = "科目最长 50 位")
    private String subject;

    @Size(max = 100, message = "知识点最长 100 位")
    private String knowledgePoint;

    /** 题型：1-单选，2-多选，3-填空，4-简答 */
    @NotNull(message = "题型不能为空")
    @Min(value = 1, message = "题型取值 1-4")
    @Max(value = 4, message = "题型取值 1-4")
    private Integer type;

    /** 难度：1-简单，2-中等，3-困难（默认中等） */
    @Min(value = 1, message = "难度取值 1-3")
    @Max(value = 3, message = "难度取值 1-3")
    private Integer difficulty = 2;

    /** 生成题量 1-10 */
    @NotNull(message = "题量不能为空")
    @Min(value = 1, message = "题量取值 1-10")
    @Max(value = 10, message = "题量取值 1-10")
    private Integer count;

    /** 每题分值（默认 5） */
    @Min(value = 1, message = "分值需大于 0")
    @Max(value = 100, message = "分值不能超过 100")
    private Integer score = 5;

    /** 是否直接保存到题库（false 仅预览） */
    private Boolean save = false;
}
