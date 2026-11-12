package org.king.smartquiz.system.controller;

import jakarta.validation.Valid;
import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.system.dto.request.AiGenerateDTO;
import org.king.smartquiz.system.service.AiQuestionService;
import org.king.smartquiz.system.vo.QuestionVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 智能出题控制器（受保护接口，需携带有效 JWT）。
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiQuestionService aiQuestionService;

    public AiController(AiQuestionService aiQuestionService) {
        this.aiQuestionService = aiQuestionService;
    }

    /**
     * AI 生成题目。dto.save=true 时直接入库，否则仅返回预览。
     */
    @Log(title = "AI出题", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public Result<List<QuestionVO>> generate(@RequestBody @Valid AiGenerateDTO dto) {
        List<QuestionVO> list = aiQuestionService.generate(dto);
        boolean saved = Boolean.TRUE.equals(dto.getSave());
        return Result.success(saved ? "AI 出题成功并已入库" : "AI 出题成功（预览，未入库）", list);
    }
}
