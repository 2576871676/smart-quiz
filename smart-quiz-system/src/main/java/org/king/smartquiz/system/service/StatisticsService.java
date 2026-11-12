package org.king.smartquiz.system.service;

import org.king.smartquiz.system.vo.AccuracyStatVO;
import org.king.smartquiz.system.vo.StudyOverviewVO;
import org.king.smartquiz.system.vo.TrendPointVO;
import org.king.smartquiz.system.vo.WeakPointVO;

import java.util.List;

/**
 * 学习统计分析服务。
 * 面向当前登录用户聚合其答题与错题数据，并由大模型产出个性化学习建议。
 */
public interface StatisticsService {

    /** 学习总览：答题量、正确率、错题本概况、近期活跃 */
    StudyOverviewVO overview();

    /** 多维度正确率：按题型 / 难度 / 科目 */
    AccuracyStatVO accuracy();

    /** 最近 N 天答题趋势（按天补零） */
    List<TrendPointVO> trend(int days);

    /** 薄弱知识点 TOP N（按答错次数降序） */
    List<WeakPointVO> weakPoints(int limit);

    /** AI 个性化学习建议（纯文本） */
    String aiAdvice();
}
