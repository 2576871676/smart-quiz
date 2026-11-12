package org.king.smartquiz.system.controller;

import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.system.service.StatisticsService;
import org.king.smartquiz.system.vo.AccuracyStatVO;
import org.king.smartquiz.system.vo.StudyOverviewVO;
import org.king.smartquiz.system.vo.TrendPointVO;
import org.king.smartquiz.system.vo.WeakPointVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习统计分析控制器（受保护接口，需携带有效 JWT）。
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /** 学习总览 */
    @GetMapping("/overview")
    public Result<StudyOverviewVO> overview() {
        return Result.success(statisticsService.overview());
    }

    /** 多维度正确率（题型 / 难度 / 科目） */
    @GetMapping("/accuracy")
    public Result<AccuracyStatVO> accuracy() {
        return Result.success(statisticsService.accuracy());
    }

    /** 最近 N 天答题趋势 */
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(statisticsService.trend(days));
    }

    /** 薄弱知识点 TOP N */
    @GetMapping("/weak-points")
    public Result<List<WeakPointVO>> weakPoints(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(statisticsService.weakPoints(limit));
    }

    /** AI 个性化学习建议 */
    @Log(title = "AI学习建议", businessType = BusinessType.OTHER)
    @GetMapping("/advice")
    public Result<String> advice() {
        return Result.success("已生成学习建议", statisticsService.aiAdvice());
    }
}
