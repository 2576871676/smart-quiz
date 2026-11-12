package org.king.smartquiz.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.framework.ai.AiClient;
import org.king.smartquiz.framework.ai.AiProperties;
import org.king.smartquiz.framework.security.UserContext;
import org.king.smartquiz.system.entity.WrongQuestion;
import org.king.smartquiz.system.mapper.StatisticsMapper;
import org.king.smartquiz.system.mapper.WrongQuestionMapper;
import org.king.smartquiz.system.service.StatisticsService;
import org.king.smartquiz.system.vo.AccuracyStatVO;
import org.king.smartquiz.system.vo.AnswerCountVO;
import org.king.smartquiz.system.vo.DimensionStatVO;
import org.king.smartquiz.system.vo.StudyOverviewVO;
import org.king.smartquiz.system.vo.TrendPointVO;
import org.king.smartquiz.system.vo.WeakPointVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习统计分析服务实现。
 * 聚合逻辑尽量下推到数据库（见 StatisticsMapper.xml），服务层只做补零、正确率换算与建议装配。
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int RECENT_DAYS = 7;

    private static final String ADVICE_SYSTEM =
            "你是一名专业的学习规划师，擅长根据学生的答题数据给出可执行的学习建议。" +
            "请用简洁的中文分点输出建议正文，语气鼓励、具体、有针对性，不要输出 JSON 或代码块标记。";

    private final StatisticsMapper statisticsMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final AiClient aiClient;
    private final AiProperties aiProperties;

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper,
                                 WrongQuestionMapper wrongQuestionMapper,
                                 AiClient aiClient,
                                 AiProperties aiProperties) {
        this.statisticsMapper = statisticsMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
    }

    @Override
    public StudyOverviewVO overview() {
        Long userId = currentUserId();

        AnswerCountVO counts = statisticsMapper.countAnswers(userId);
        int total = counts == null || counts.getTotalCount() == null ? 0 : counts.getTotalCount();
        int correct = counts == null || counts.getCorrectCount() == null ? 0 : counts.getCorrectCount();
        int wrong = counts == null || counts.getWrongCount() == null ? 0 : counts.getWrongCount();
        int ungraded = counts == null || counts.getUngradedCount() == null ? 0 : counts.getUngradedCount();

        StudyOverviewVO vo = new StudyOverviewVO();
        vo.setTotalAnswered(total);
        vo.setCorrectCount(correct);
        vo.setWrongCount(wrong);
        vo.setUngradedCount(ungraded);
        vo.setAccuracy(rate(correct, total));

        vo.setWrongBookCount((int) (long) wrongQuestionMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId)));
        vo.setMasteredCount((int) (long) wrongQuestionMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getMastered, 1)));

        vo.setRecentDays(RECENT_DAYS);
        Integer recent = statisticsMapper.countRecent(userId, LocalDate.now().minusDays(RECENT_DAYS - 1L).atStartOfDay());
        vo.setRecentAnswered(recent == null ? 0 : recent);
        return vo;
    }

    @Override
    public AccuracyStatVO accuracy() {
        Long userId = currentUserId();
        AccuracyStatVO vo = new AccuracyStatVO();
        vo.setByType(fillAccuracy(statisticsMapper.accuracyByType(userId)));
        vo.setByDifficulty(fillAccuracy(statisticsMapper.accuracyByDifficulty(userId)));
        vo.setBySubject(fillAccuracy(statisticsMapper.accuracyBySubject(userId)));
        return vo;
    }

    @Override
    public List<TrendPointVO> trend(int days) {
        Long userId = currentUserId();
        int span = days < 1 ? 1 : Math.min(days, 90);
        LocalDate start = LocalDate.now().minusDays(span - 1L);
        LocalDateTime since = start.atStartOfDay();

        Map<String, TrendPointVO> byDay = new LinkedHashMap<>();
        for (TrendPointVO p : statisticsMapper.trend(userId, since)) {
            p.setAccuracy(rate(p.getCorrect(), p.getTotal()));
            byDay.put(p.getDate(), p);
        }

        List<TrendPointVO> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            String day = start.plusDays(i).format(DAY);
            TrendPointVO p = byDay.get(day);
            if (p == null) {
                p = new TrendPointVO();
                p.setDate(day);
                p.setTotal(0);
                p.setCorrect(0);
                p.setAccuracy(0.0);
            }
            result.add(p);
        }
        return result;
    }

    @Override
    public List<WeakPointVO> weakPoints(int limit) {
        Long userId = currentUserId();
        int size = limit < 1 ? 5 : Math.min(limit, 20);
        return statisticsMapper.weakPoints(userId, size);
    }

    @Override
    public String aiAdvice() {
        Long userId = currentUserId();
        String userPrompt = buildAdvicePrompt(userId);
        int maxRetry = Math.max(1, aiProperties.getMaxRetry());
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                String text = aiClient.chat(ADVICE_SYSTEM, userPrompt);
                if (StrUtil.isNotBlank(text)) {
                    return text.trim();
                }
                last = new BusinessException(ResultCode.AI_PARSE_ERROR, "AI 未返回有效建议");
            } catch (BusinessException e) {
                throw e;
            } catch (RuntimeException e) {
                last = e;
                log.warn("第 {} 次生成学习建议失败: {}", attempt, e.getMessage());
            }
        }
        throw last != null ? last : new BusinessException(ResultCode.AI_ERROR, "生成学习建议失败");
    }

    /** 装配建议所需的学习数据快照。提示词含标记「学习建议」「正确率：x%」，Mock 客户端据此识别场景。 */
    private String buildAdvicePrompt(Long userId) {
        AnswerCountVO counts = statisticsMapper.countAnswers(userId);
        int total = counts == null || counts.getTotalCount() == null ? 0 : counts.getTotalCount();
        int correct = counts == null || counts.getCorrectCount() == null ? 0 : counts.getCorrectCount();
        int wrong = counts == null || counts.getWrongCount() == null ? 0 : counts.getWrongCount();
        int ungraded = counts == null || counts.getUngradedCount() == null ? 0 : counts.getUngradedCount();
        double accuracy = rate(correct, total);

        int wrongBook = (int) (long) wrongQuestionMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getMastered, 0));
        Integer recent = statisticsMapper.countRecent(userId, LocalDate.now().minusDays(RECENT_DAYS - 1L).atStartOfDay());
        int recentAnswered = recent == null ? 0 : recent;

        List<DimensionStatVO> bySubject = fillAccuracy(statisticsMapper.accuracyBySubject(userId));
        List<WeakPointVO> weak = statisticsMapper.weakPoints(userId, 5);

        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下学习数据，为该学生给出个性化的学习建议。\n");
        sb.append("总答题数：").append(total).append("\n");
        sb.append("总正确率：").append(accuracy).append("%\n");
        sb.append("答对：").append(correct).append("，答错：").append(wrong).append("，未判分：").append(ungraded).append("\n");
        sb.append("错题本未掌握数：").append(wrongBook).append("\n");
        sb.append("最近").append(RECENT_DAYS).append("天答题数：").append(recentAnswered).append("\n");
        sb.append("各科目正确率：");
        if (bySubject.isEmpty()) {
            sb.append("暂无数据");
        } else {
            for (DimensionStatVO d : bySubject) {
                sb.append(d.getName()).append(" ").append(d.getAccuracy()).append("%；");
            }
        }
        sb.append("\n薄弱知识点TOP5：");
        if (weak.isEmpty()) {
            sb.append("暂无明显薄弱点");
        } else {
            for (WeakPointVO w : weak) {
                sb.append(w.getKnowledgePoint()).append("（错").append(w.getWrongCount()).append("次）；");
            }
        }
        sb.append("\n请输出 3-5 条具体、可执行的学习建议。");
        return sb.toString();
    }

    /** 为维度统计补齐正确率字段（数据库侧只聚合 total/correct）。 */
    private List<DimensionStatVO> fillAccuracy(List<DimensionStatVO> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        for (DimensionStatVO d : list) {
            int t = d.getTotal() == null ? 0 : d.getTotal();
            int c = d.getCorrect() == null ? 0 : d.getCorrect();
            d.setAccuracy(rate(c, t));
        }
        return list;
    }

    /** 正确率百分比，保留 1 位小数；分母为 0 返回 0.0 */
    private double rate(int correct, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(correct * 1000.0 / total) / 10.0;
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
