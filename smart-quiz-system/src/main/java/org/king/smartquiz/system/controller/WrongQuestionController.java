package org.king.smartquiz.system.controller;

import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.system.dto.request.WrongQuestionQueryDTO;
import org.king.smartquiz.system.service.WrongQuestionService;
import org.king.smartquiz.system.vo.WrongQuestionVO;
import org.king.smartquiz.system.vo.WrongStatVO;
import org.springframework.web.bind.annotation.*;

/**
 * 错题本控制器（受保护接口，需携带有效 JWT）。
 * 错题重做复用 /answer/submit：再次答对会自动标记为已掌握。
 */
@RestController
@RequestMapping("/wrong-question")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    /** 错题列表（分页 + 科目/知识点/掌握状态筛选） */
    @GetMapping("/page")
    public Result<PageResult<WrongQuestionVO>> page(WrongQuestionQueryDTO query) {
        return Result.success(wrongQuestionService.page(query));
    }

    /** 错题统计（总量、掌握率、科目分布、薄弱知识点 TOP5） */
    @GetMapping("/statistics")
    public Result<WrongStatVO> statistics() {
        return Result.success(wrongQuestionService.statistics());
    }

    /** 标记某错题为已掌握 */
    @Log(title = "错题本", businessType = BusinessType.UPDATE)
    @PutMapping("/{questionId}/master")
    public Result<Void> master(@PathVariable Long questionId) {
        wrongQuestionService.markMastered(questionId);
        return Result.success("已标记为掌握", null);
    }

    /** 从错题本移除某题 */
    @Log(title = "错题本", businessType = BusinessType.DELETE)
    @DeleteMapping("/{questionId}")
    public Result<Void> remove(@PathVariable Long questionId) {
        wrongQuestionService.remove(questionId);
        return Result.success("已移出错题本", null);
    }
}
