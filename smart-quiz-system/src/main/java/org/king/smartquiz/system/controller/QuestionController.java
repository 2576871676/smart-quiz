package org.king.smartquiz.system.controller;

import jakarta.validation.Valid;
import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.system.dto.request.QuestionDTO;
import org.king.smartquiz.system.dto.request.QuestionQueryDTO;
import org.king.smartquiz.system.service.QuestionService;
import org.king.smartquiz.system.vo.QuestionVO;
import org.springframework.web.bind.annotation.*;

/**
 * 题目管理控制器（受保护接口，需携带有效 JWT）。
 */
@RestController
@RequestMapping("/question")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /** 新增题目 */
    @Log(title = "题目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid QuestionDTO dto) {
        return Result.success("题目新增成功", questionService.addQuestion(dto));
    }

    /** 修改题目 */
    @Log(title = "题目管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid QuestionDTO dto) {
        questionService.updateQuestion(id, dto);
        return Result.success("题目修改成功", null);
    }

    /** 删除题目 */
    @Log(title = "题目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.success("题目删除成功", null);
    }

    /** 题目详情 */
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.success(questionService.getQuestionDetail(id));
    }

    /** 分页查询（查询参数绑定，非请求体） */
    @GetMapping("/page")
    public Result<PageResult<QuestionVO>> page(QuestionQueryDTO query) {
        return Result.success(questionService.pageQuery(query));
    }
}
