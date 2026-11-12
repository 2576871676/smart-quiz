package org.king.smartquiz.system.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 试卷详情（管理端）：基本信息 + 题目清单（含答案解析）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExamPaperDetailVO extends ExamPaperVO {

    private List<ExamPaperQuestionVO> items;
}
