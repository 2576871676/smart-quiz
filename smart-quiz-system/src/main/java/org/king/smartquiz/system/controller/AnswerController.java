package org.king.smartquiz.system.controller;

import jakarta.validation.Valid;
import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.system.dto.request.SubmitAnswerDTO;
import org.king.smartquiz.system.service.AnswerService;
import org.king.smartquiz.system.vo.AnswerRecordVO;
import org.king.smartquiz.system.vo.AnswerResultVO;
import org.springframework.web.bind.annotation.*;

/**
 * 在线答题与自动批改控制器（受保护接口，需携带有效 JWT）。
 */
@RestController
@RequestMapping("/answer")
public class AnswerController {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }

    /** 提交答卷并自动批改 */
    @Log(title = "在线答题", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public Result<AnswerResultVO> submit(@RequestBody @Valid SubmitAnswerDTO dto) {
        return Result.success("批改完成", answerService.submit(dto));
    }

    /** 我的答题记录（分页） */
    @GetMapping("/records")
    public Result<PageResult<AnswerRecordVO>> records(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(answerService.myRecords(pageNum, pageSize));
    }
}
