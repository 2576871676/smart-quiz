package org.king.smartquiz.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.framework.security.UserContext;
import org.king.smartquiz.system.dto.request.AnswerItemDTO;
import org.king.smartquiz.system.dto.request.SubmitAnswerDTO;
import org.king.smartquiz.system.entity.AnswerRecord;
import org.king.smartquiz.system.entity.Question;
import org.king.smartquiz.system.mapper.AnswerRecordMapper;
import org.king.smartquiz.system.mapper.QuestionMapper;
import org.king.smartquiz.system.service.AnswerService;
import org.king.smartquiz.system.service.QuestionGrader;
import org.king.smartquiz.system.service.WrongQuestionService;
import org.king.smartquiz.system.vo.AnswerDetailVO;
import org.king.smartquiz.system.vo.AnswerRecordVO;
import org.king.smartquiz.system.vo.AnswerResultVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在线答题（练习模式）服务实现。
 * 判分统一委托 {@link QuestionGrader}（与试卷模式共用同一判分口径）；
 * 本类负责落库 answer_record、错题本联动与汇总。
 */
@Slf4j
@Service
public class AnswerServiceImpl implements AnswerService {

    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionMapper questionMapper;
    private final QuestionGrader grader;
    private final WrongQuestionService wrongQuestionService;

    public AnswerServiceImpl(AnswerRecordMapper answerRecordMapper, QuestionMapper questionMapper,
                             QuestionGrader grader, WrongQuestionService wrongQuestionService) {
        this.answerRecordMapper = answerRecordMapper;
        this.questionMapper = questionMapper;
        this.grader = grader;
        this.wrongQuestionService = wrongQuestionService;
    }

    @Override
    public AnswerResultVO submit(SubmitAnswerDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        List<AnswerItemDTO> items = dto.getAnswers();

        // 批量加载题目，缺失即报错
        List<Long> ids = items.stream().map(AnswerItemDTO::getQuestionId).distinct().collect(Collectors.toList());
        Map<Long, Question> qMap = questionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        int obtainedScore = 0, totalScore = 0, correct = 0, wrong = 0, ungraded = 0;
        List<AnswerDetailVO> details = new ArrayList<>();

        for (AnswerItemDTO item : items) {
            Question q = qMap.get(item.getQuestionId());
            if (q == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在: " + item.getQuestionId());
            }

            // 统一判分（客观题本地 / 主观题 AI）
            QuestionGrader.GradeOutcome go = grader.grade(q, item.getUserAnswer());
            int full = go.fullScore();
            int score = go.score();
            Integer isCorrect = go.isCorrect();
            String aiComment = go.comment();

            // 落库答题记录（练习模式 paperId 为空）
            AnswerRecord record = new AnswerRecord();
            record.setUserId(userId);
            record.setQuestionId(q.getId());
            record.setUserAnswer(item.getUserAnswer());
            record.setIsCorrect(isCorrect);
            record.setScore(score);
            record.setAiComment(aiComment);
            record.setDuration(item.getDuration() == null ? 0 : item.getDuration());
            answerRecordMapper.insert(record);

            // 错题本联动：答错则收录/累加，答对则若曾在错题本标记为已掌握（未判分不处理）
            if (isCorrect != null) {
                if (isCorrect == 0) {
                    wrongQuestionService.collectWrong(userId, q.getId());
                } else {
                    wrongQuestionService.markMasteredIfCorrect(userId, q.getId());
                }
            }

            // 汇总
            totalScore += full;
            obtainedScore += score;
            if (isCorrect == null) {
                ungraded++;
            } else if (isCorrect == 1) {
                correct++;
            } else {
                wrong++;
            }

            AnswerDetailVO detail = new AnswerDetailVO();
            detail.setQuestionId(q.getId());
            detail.setTitle(q.getTitle());
            detail.setType(q.getType());
            detail.setDifficulty(q.getDifficulty());
            detail.setOptions(q.getOptions());
            detail.setUserAnswer(item.getUserAnswer());
            detail.setReferenceAnswer(q.getAnswer());
            detail.setAnalysis(q.getAnalysis());
            detail.setIsCorrect(isCorrect);
            detail.setScore(score);
            detail.setFullScore(full);
            detail.setAiComment(aiComment);
            details.add(detail);
        }

        int total = items.size();
        AnswerResultVO result = new AnswerResultVO();
        result.setObtainedScore(obtainedScore);
        result.setTotalScore(totalScore);
        result.setCorrectCount(correct);
        result.setWrongCount(wrong);
        result.setUngradedCount(ungraded);
        result.setTotalCount(total);
        result.setAccuracy(total == 0 ? 0.0 : Math.round(correct * 1000.0 / total) / 10.0);
        result.setDetails(details);
        return result;
    }

    @Override
    public PageResult<AnswerRecordVO> myRecords(int pageNum, int pageSize) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        int pn = pageNum < 1 ? 1 : pageNum;
        int ps = pageSize < 1 ? 10 : pageSize;
        Page<AnswerRecord> page = answerRecordMapper.selectPage(new Page<>(pn, ps),
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getUserId, userId)
                        .orderByDesc(AnswerRecord::getCreateTime)
                        .orderByDesc(AnswerRecord::getId));

        List<AnswerRecord> records = page.getRecords();
        Map<Long, Question> qMap = records.isEmpty() ? Map.of()
                : questionMapper.selectBatchIds(records.stream().map(AnswerRecord::getQuestionId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Question::getId, Function.identity()));

        List<AnswerRecordVO> vos = records.stream().map(r -> {
            AnswerRecordVO vo = new AnswerRecordVO();
            vo.setId(r.getId());
            vo.setQuestionId(r.getQuestionId());
            vo.setUserAnswer(r.getUserAnswer());
            vo.setIsCorrect(r.getIsCorrect());
            vo.setScore(r.getScore());
            vo.setAiComment(r.getAiComment());
            vo.setDuration(r.getDuration());
            vo.setCreateTime(r.getCreateTime());
            Question q = qMap.get(r.getQuestionId());
            if (q != null) {
                vo.setTitle(q.getTitle());
                vo.setSubject(q.getSubject());
                vo.setType(q.getType());
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), vos);
    }
}
