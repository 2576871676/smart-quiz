package org.king.smartquiz.system.controller;

import jakarta.validation.Valid;
import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.system.dto.request.ExamPaperQueryDTO;
import org.king.smartquiz.system.dto.request.SaveExamPaperDTO;
import org.king.smartquiz.system.dto.request.SubmitExamDTO;
import org.king.smartquiz.system.service.ExamService;
import org.king.smartquiz.system.vo.ExamPaperDetailVO;
import org.king.smartquiz.system.vo.ExamPaperVO;
import org.king.smartquiz.system.vo.ExamRecordVO;
import org.king.smartquiz.system.vo.ExamResultVO;
import org.king.smartquiz.system.vo.ExamSessionVO;
import org.springframework.web.bind.annotation.*;

/**
 * 试卷管理与试卷模式答题控制器（受保护接口，需携带有效 JWT）。
 */
@RestController
@RequestMapping("/exam")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /** 新建试卷 */
    @Log(title = "试卷管理", businessType = BusinessType.INSERT)
    @PostMapping("/paper")
    public Result<Long> createPaper(@RequestBody @Valid SaveExamPaperDTO dto) {
        return Result.success("试卷创建成功", examService.createPaper(dto));
    }

    /** 修改试卷 */
    @Log(title = "试卷管理", businessType = BusinessType.UPDATE)
    @PutMapping("/paper/{id}")
    public Result<Void> updatePaper(@PathVariable Long id, @RequestBody @Valid SaveExamPaperDTO dto) {
        examService.updatePaper(id, dto);
        return Result.success("试卷修改成功", null);
    }

    /** 删除试卷 */
    @Log(title = "试卷管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/paper/{id}")
    public Result<Void> deletePaper(@PathVariable Long id) {
        examService.deletePaper(id);
        return Result.success("试卷删除成功", null);
    }

    /** 分页查询试卷（查询参数绑定，非请求体） */
    @GetMapping("/paper/page")
    public Result<PageResult<ExamPaperVO>> pagePapers(ExamPaperQueryDTO query) {
        return Result.success(examService.pagePapers(query));
    }

    /** 试卷详情（含答案与解析） */
    @GetMapping("/paper/{id}")
    public Result<ExamPaperDetailVO> paperDetail(@PathVariable Long id) {
        return Result.success(examService.getPaperDetail(id));
    }

    /** 发布试卷 */
    @Log(title = "试卷管理", businessType = BusinessType.UPDATE)
    @PutMapping("/paper/{id}/publish")
    public Result<Void> publishPaper(@PathVariable Long id) {
        examService.publishPaper(id);
        return Result.success("试卷已发布", null);
    }

    /** 开考：创建考试会话并下发题目（不含答案） */
    @Log(title = "试卷答题", businessType = BusinessType.INSERT)
    @PostMapping("/start/{paperId}")
    public Result<ExamSessionVO> startExam(@PathVariable Long paperId) {
        return Result.success("开始考试", examService.startExam(paperId));
    }

    /** 交卷并自动批改 */
    @Log(title = "试卷答题", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public Result<ExamResultVO> submitExam(@RequestBody @Valid SubmitExamDTO dto) {
        return Result.success("交卷成功", examService.submitExam(dto));
    }

    /** 我的考试记录（分页） */
    @GetMapping("/records")
    public Result<PageResult<ExamRecordVO>> records(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(examService.myExamRecords(pageNum, pageSize));
    }
}
