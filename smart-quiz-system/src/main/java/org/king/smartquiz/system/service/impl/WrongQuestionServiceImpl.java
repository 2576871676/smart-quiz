package org.king.smartquiz.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.framework.security.UserContext;
import org.king.smartquiz.system.dto.request.WrongQuestionQueryDTO;
import org.king.smartquiz.system.entity.Question;
import org.king.smartquiz.system.entity.WrongQuestion;
import org.king.smartquiz.system.mapper.QuestionMapper;
import org.king.smartquiz.system.mapper.WrongQuestionMapper;
import org.king.smartquiz.system.service.WrongQuestionService;
import org.king.smartquiz.system.vo.WrongQuestionVO;
import org.king.smartquiz.system.vo.WrongStatVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题本服务实现。
 */
@Slf4j
@Service
public class WrongQuestionServiceImpl implements WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;

    public WrongQuestionServiceImpl(WrongQuestionMapper wrongQuestionMapper, QuestionMapper questionMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    public void collectWrong(Long userId, Long questionId) {
        WrongQuestion existing = find(userId, questionId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            WrongQuestion w = new WrongQuestion();
            w.setUserId(userId);
            w.setQuestionId(questionId);
            w.setWrongCount(1);
            w.setMastered(0);
            w.setLastWrongTime(now);
            wrongQuestionMapper.insert(w);
        } else {
            existing.setWrongCount((existing.getWrongCount() == null ? 0 : existing.getWrongCount()) + 1);
            existing.setLastWrongTime(now);
            existing.setMastered(0); // 再次答错，取消已掌握标记
            wrongQuestionMapper.updateById(existing);
        }
    }

    @Override
    public void markMasteredIfCorrect(Long userId, Long questionId) {
        WrongQuestion existing = find(userId, questionId);
        if (existing != null && !Integer.valueOf(1).equals(existing.getMastered())) {
            existing.setMastered(1);
            wrongQuestionMapper.updateById(existing);
        }
    }

    @Override
    public PageResult<WrongQuestionVO> page(WrongQuestionQueryDTO query) {
        Long userId = currentUserId();
        int pn = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int ps = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, userId);

        // 科目/知识点属于 question 表，先查出匹配的题目ID再过滤，保证分页在 DB 侧完成
        if (StrUtil.isNotBlank(query.getSubject()) || StrUtil.isNotBlank(query.getKnowledgePoint())) {
            LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<>();
            qw.select(Question::getId)
                    .eq(StrUtil.isNotBlank(query.getSubject()), Question::getSubject, query.getSubject())
                    .eq(StrUtil.isNotBlank(query.getKnowledgePoint()), Question::getKnowledgePoint, query.getKnowledgePoint());
            List<Long> qids = questionMapper.selectList(qw).stream().map(Question::getId).collect(Collectors.toList());
            if (qids.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(WrongQuestion::getQuestionId, qids);
        }
        if (query.getMastered() != null) {
            wrapper.eq(WrongQuestion::getMastered, query.getMastered() ? 1 : 0);
        }
        if ("wrongCount".equals(query.getSortBy())) {
            wrapper.orderByDesc(WrongQuestion::getWrongCount);
        } else {
            wrapper.orderByDesc(WrongQuestion::getLastWrongTime);
        }

        Page<WrongQuestion> page = wrongQuestionMapper.selectPage(new Page<>(pn, ps), wrapper);
        List<WrongQuestion> records = page.getRecords();
        Map<Long, Question> qMap = loadQuestions(records.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toList()));

        List<WrongQuestionVO> vos = records.stream().map(r -> {
            WrongQuestionVO vo = new WrongQuestionVO();
            vo.setId(r.getId());
            vo.setQuestionId(r.getQuestionId());
            vo.setWrongCount(r.getWrongCount());
            vo.setMastered(r.getMastered());
            vo.setLastWrongTime(r.getLastWrongTime());
            Question q = qMap.get(r.getQuestionId());
            if (q != null) {
                vo.setSubject(q.getSubject());
                vo.setKnowledgePoint(q.getKnowledgePoint());
                vo.setType(q.getType());
                vo.setDifficulty(q.getDifficulty());
                vo.setTitle(q.getTitle());
                vo.setOptions(q.getOptions());
                vo.setAnswer(q.getAnswer());
                vo.setAnalysis(q.getAnalysis());
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), vos);
    }

    @Override
    public WrongStatVO statistics() {
        Long userId = currentUserId();
        List<WrongQuestion> all = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));

        WrongStatVO vo = new WrongStatVO();
        vo.setTotalCount(all.size());
        int mastered = (int) all.stream().filter(w -> Integer.valueOf(1).equals(w.getMastered())).count();
        vo.setMasteredCount(mastered);
        vo.setMasteryRate(all.isEmpty() ? 0.0 : Math.round(mastered * 1000.0 / all.size()) / 10.0);

        Map<Long, Question> qMap = loadQuestions(all.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toList()));

        Map<String, Integer> subjectCount = new LinkedHashMap<>();
        // 知识点 -> [累计错误次数, 题目数]
        Map<String, int[]> pointAgg = new LinkedHashMap<>();
        for (WrongQuestion w : all) {
            Question q = qMap.get(w.getQuestionId());
            String subject = q != null && StrUtil.isNotBlank(q.getSubject()) ? q.getSubject() : "未知";
            subjectCount.merge(subject, 1, Integer::sum);
            String kp = q != null && StrUtil.isNotBlank(q.getKnowledgePoint()) ? q.getKnowledgePoint() : "未分类";
            int[] agg = pointAgg.computeIfAbsent(kp, k -> new int[2]);
            agg[0] += w.getWrongCount() == null ? 1 : w.getWrongCount();
            agg[1] += 1;
        }

        vo.setSubjectStats(subjectCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new WrongStatVO.SubjectStat(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));

        vo.setWeakPoints(pointAgg.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[0]).reversed())
                .limit(5)
                .map(e -> new WrongStatVO.WeakPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList()));

        return vo;
    }

    @Override
    public void markMastered(Long questionId) {
        Long userId = currentUserId();
        WrongQuestion w = find(userId, questionId);
        if (w == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "错题不存在");
        }
        if (!Integer.valueOf(1).equals(w.getMastered())) {
            w.setMastered(1);
            wrongQuestionMapper.updateById(w);
        }
    }

    @Override
    public void remove(Long questionId) {
        Long userId = currentUserId();
        wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId));
    }

    private WrongQuestion find(Long userId, Long questionId) {
        return wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId));
    }

    private Map<Long, Question> loadQuestions(List<Long> questionIds) {
        List<Long> ids = questionIds.stream().distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return questionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
