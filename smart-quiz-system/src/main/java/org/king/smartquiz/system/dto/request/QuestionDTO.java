package org.king.smartquiz.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 题目新增 / 修改入参。
 */
@Data
public class QuestionDTO {

    @NotBlank(message = "科目不能为空")
    @Size(max = 50, message = "科目最长 50 位")
    private String subject;

    @Size(max = 100, message = "知识点最长 100 位")
    private String knowledgePoint;

    /** 题型：1-单选，2-多选，3-填空，4-简答 */
    @NotNull(message = "题型不能为空")
    private Integer type;

    /** 难度：1-简单，2-中等，3-困难 */
    private Integer difficulty;

    @NotBlank(message = "题干不能为空")
    private String title;

    /** 选项（JSON 字符串，选择题用） */
    private String options;

    @NotBlank(message = "参考答案不能为空")
    private String answer;

    private String analysis;

    private Integer score;

    /** 来源：1-AI生成，2-手动录入（不传默认手动） */
    private Integer source;
}
