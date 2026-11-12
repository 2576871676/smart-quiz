package org.king.smartquiz.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.framework.security.LoginUser;
import org.king.smartquiz.framework.security.UserContext;
import org.king.smartquiz.system.dto.request.AnswerItemDTO;
import org.king.smartquiz.system.dto.request.ExamPaperItemDTO;
import org.king.smartquiz.system.dto.request.ExamPaperQueryDTO;
import org.king.smartquiz.system.dto.request.SaveExamPaperDTO;
import org.king.smartquiz.system.dto.request.SubmitExamDTO;
import org.king.smartquiz.system.entity.AnswerRecord;
import org.king.smartquiz.system.entity.ExamPaper;
import org.king.smartquiz.system.entity.ExamPaperQuestion;
import org.king.smartquiz.system.entity.ExamRecord;
import org.king.smartquiz.system.entity.Question;
import org.king.smartquiz.system.mapper.AnswerRecordMapper;
import org.king.smartquiz.system.mapper.ExamPaperMapper;
import org.king.smartquiz.system.mapper.ExamPaperQuestionMapper;
import org.king.smartquiz.system.mapper.ExamRecordMapper;
import org.king.smartquiz.system.mapper.QuestionMapper;
import org.king.smartquiz.system.service.ExamService;
import org.king.smartquiz.system.service.QuestionGrader;
import org.king.smartquiz.system.service.WrongQuestionService;
import org.king.smartquiz.system.vo.AnswerDetailVO;
import org.king.smartquiz.system.vo.ExamPaperDetailVO;
import org.king.smartquiz.system.vo.ExamPaperQuestionVO;
import org.king.smartquiz.system.vo.ExamPaperVO;
import org.king.smartquiz.system.vo.ExamQuestionVO;
import org.king.smartquiz.system.vo.ExamRecordVO;
import org.king.smartquiz.system.vo.ExamResultVO;
import org.king.smartquiz.system.vo.ExamSessionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 试卷管理与试卷模式答题服务实现。
 */
@Slf4j
@Service
public class ExamServiceImpl implements ExamService {

    private static final int ROLE_ADMIN = 1;
    private static final int DEFAULT_SCORE = 5;
    private static final int DEFAULT_DURATION = 60;
    /** 试卷状态：0-草稿，1-已发布 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    /** 考试记录状态：0-进行中，1-已提交 */
    private static final int RECORD_ONGOING = 0;
    private static final int RECORD_SUBMITTED = 1;

    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionMapper examPaperQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionGrader grader;
    private final WrongQuestionService wrongQuestionService;

    public ExamServiceImpl(ExamPaperMapper examPaperMapper, ExamPaperQuestionMapper examPaperQuestionMapper,
                           ExamRecordMapper examRecordMapper, QuestionMapper questionMapper,
                           AnswerRecordMapper answerRecordMapper, QuestionGrader grader,
                           WrongQuestionService wrongQuestionService) {
        this.examPaperMapper = examPaperMapper;
        this.examPaperQuestionMapper = examPaperQuestionMapper;
        this.examRecordMapper = examRecordMapper;
        this.questionMapper = questionMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.grader = grader;
        this.wrongQuestionService = wrongQuestionService;
    }

    // ==================== 试卷管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPaper(SaveExamPaperDTO dto) {
        Long userId = currentUserId();
        ExamPaper paper = new ExamPaper();
        paper.setUserId(userId);
        paper.setTitle(dto.getTitle());
        paper.setSubject(dto.getSubject());
        paper.setDescription(dto.getDescription());
        paper.setDuration(dto.getDuration() == null ? DEFAULT_DURATION : dto.getDuration());
        paper.setStatus(dto.getStatus() == null ? STATUS_DRAFT : dto.getStatus());
        paper.setTotalScore(0);
        paper.setQuestionCount(0);
        examPaperMapper.insert(paper);

        int totalScore = saveItems(paper.getId(), dto.getItems());
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(dto.getItems().size());
        examPaperMapper.updateById(paper);
        return paper.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaper(Long id, SaveExamPaperDTO dto) {
        ExamPaper paper = requirePaper(id);
        checkOwner(paper);

        ExamPaper update = new ExamPaper();
        update.setId(id);
        update.setTitle(dto.getTitle());
        update.setSubject(dto.getSubject());
        update.setDescription(dto.getDescription());
        if (dto.getDuration() != null) {
            update.setDuration(dto.getDuration());
        }
        if (dto.getStatus() != null) {
            update.setStatus(dto.getStatus());
        }

        // 覆盖题目清单
        examPaperQuestionMapper.delete(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, id));
        int totalScore = saveItems(id, dto.getItems());
        update.setTotalScore(totalScore);
        update.setQuestionCount(dto.getItems().size());
        examPaperMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaper(Long id) {
        ExamPaper paper = requirePaper(id);
        checkOwner(paper);
        examPaperQuestionMapper.delete(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, id));
        examPaperMapper.deleteById(id);
    }

    @Override
    public PageResult<ExamPaperVO> pagePapers(ExamPaperQueryDTO query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(query.getSubject()), ExamPaper::getSubject, query.getSubject())
                .eq(query.getStatus() != null, ExamPaper::getStatus, query.getStatus())
                .like(StrUtil.isNotBlank(query.getKeyword()), ExamPaper::getTitle, query.getKeyword())
                .eq(Boolean.TRUE.equals(query.getOnlyMine()), ExamPaper::getUserId, currentUserId())
                .orderByDesc(ExamPaper::getCreateTime)
                .orderByDesc(ExamPaper::getId);

        Page<ExamPaper> page = examPaperMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ExamPaperVO> records = page.getRecords().stream().map(this::toPaperVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public ExamPaperDetailVO getPaperDetail(Long id) {
        ExamPaper paper = requirePaper(id);
        ExamPaperDetailVO vo = new ExamPaperDetailVO();
        copyPaper(paper, vo);

        List<ExamPaperQuestion> items = listItems(id);
        Map<Long, Question> qMap = loadQuestions(items.stream()
                .map(ExamPaperQuestion::getQuestionId).collect(Collectors.toList()));

        List<ExamPaperQuestionVO> itemVOs = new ArrayList<>();
        for (ExamPaperQuestion epq : items) {
            Question q = qMap.get(epq.getQuestionId());
            if (q == null) {
                continue;
            }
            ExamPaperQuestionVO iv = new ExamPaperQuestionVO();
            iv.setQuestionId(q.getId());
            iv.setSort(epq.getSort());
            iv.setScore(epq.getScore());
            iv.setTitle(q.getTitle());
            iv.setType(q.getType());
            iv.setDifficulty(q.getDifficulty());
            iv.setOptions(q.getOptions());
            iv.setAnswer(q.getAnswer());
            iv.setAnalysis(q.getAnalysis());
            itemVOs.add(iv);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    @Override
    public void publishPaper(Long id) {
        ExamPaper paper = requirePaper(id);
        checkOwner(paper);
        if (paper.getQuestionCount() == null || paper.getQuestionCount() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "试卷至少包含一道题才能发布");
        }
        ExamPaper update = new ExamPaper();
        update.setId(id);
        update.setStatus(STATUS_PUBLISHED);
        examPaperMapper.updateById(update);
    }

    /** 落库题目清单，返回汇总分值 */
    private int saveItems(Long paperId, List<ExamPaperItemDTO> items) {
        Map<Long, Question> qMap = loadQuestions(items.stream()
                .map(ExamPaperItemDTO::getQuestionId).collect(Collectors.toList()));
        int totalScore = 0;
        int sort = 1;
        for (ExamPaperItemDTO item : items) {
            Question q = qMap.get(item.getQuestionId());
            if (q == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在: " + item.getQuestionId());
            }
            int score = item.getScore() != null ? item.getScore()
                    : (q.getScore() == null ? DEFAULT_SCORE : q.getScore());
            totalScore += score;

            ExamPaperQuestion epq = new ExamPaperQuestion();
            epq.setPaperId(paperId);
            epq.setQuestionId(item.getQuestionId());
            epq.setSort(sort++);
            epq.setScore(score);
            examPaperQuestionMapper.insert(epq);
        }
        return totalScore;
    }

    // ==================== 试卷模式答题 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamSessionVO startExam(Long paperId) {
        Long userId = currentUserId();
        ExamPaper paper = requirePaper(paperId);
        if (paper.getStatus() == null || paper.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "试卷未发布，无法开考");
        }

        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setPaperId(paperId);
        record.setTotalScore(0);
        record.setCorrectCount(0);
        record.setWrongCount(0);
        record.setDuration(0);
        record.setStatus(RECORD_ONGOING);
        record.setStartTime(LocalDateTime.now());
        examRecordMapper.insert(record);

        List<ExamPaperQuestion> items = listItems(paperId);
        Map<Long, Question> qMap = loadQuestions(items.stream()
                .map(ExamPaperQuestion::getQuestionId).collect(Collectors.toList()));

        List<ExamQuestionVO> questions = new ArrayList<>();
        for (ExamPaperQuestion epq : items) {
            Question q = qMap.get(epq.getQuestionId());
            if (q == null) {
                continue;
            }
            ExamQuestionVO qv = new ExamQuestionVO();
            qv.setQuestionId(q.getId());
            qv.setSort(epq.getSort());
            qv.setScore(epq.getScore());
            qv.setType(q.getType());
            qv.setDifficulty(q.getDifficulty());
            qv.setTitle(q.getTitle());
            qv.setOptions(q.getOptions());
            questions.add(qv);
        }

        ExamSessionVO vo = new ExamSessionVO();
        vo.setRecordId(record.getId());
        vo.setPaperId(paperId);
        vo.setTitle(paper.getTitle());
        vo.setSubject(paper.getSubject());
        vo.setDuration(paper.getDuration());
        vo.setStartTime(record.getStartTime());
        vo.setQuestions(questions);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamResultVO submitExam(SubmitExamDTO dto) {
        Long userId = currentUserId();
        ExamRecord record = examRecordMapper.selectById(dto.getRecordId());
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "考试记录不存在");
        }
        if (!userId.equals(record.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权提交他人考试");
        }
        if (record.getStatus() != null && record.getStatus() == RECORD_SUBMITTED) {
            throw new BusinessException(ResultCode.REPEAT_SUBMIT, "试卷已提交，请勿重复交卷");
        }

        // 以试卷题目为准（防漏答/防注入非试卷题目），答案按题号匹配
        List<ExamPaperQuestion> items = listItems(record.getPaperId());
        Map<Long, Question> qMap = loadQuestions(items.stream()
                .map(ExamPaperQuestion::getQuestionId).collect(Collectors.toList()));
        Map<Long, AnswerItemDTO> answerMap = dto.getAnswers().stream()
                .collect(Collectors.toMap(AnswerItemDTO::getQuestionId, Function.identity(), (a, b) -> a));

        LocalDateTime submitTime = LocalDateTime.now();
        int obtained = 0, totalScore = 0, correct = 0, wrong = 0, ungraded = 0;
        List<AnswerDetailVO> details = new ArrayList<>();

        for (ExamPaperQuestion epq : items) {
            Question q = qMap.get(epq.getQuestionId());
            if (q == null) {
                continue;
            }
            AnswerItemDTO ans = answerMap.get(epq.getQuestionId());
            String userAnswer = ans == null ? null : ans.getUserAnswer();
            int perDuration = ans == null || ans.getDuration() == null ? 0 : ans.getDuration();

            QuestionGrader.GradeOutcome go = grader.grade(q, userAnswer, epq.getScore());
            int full = go.fullScore();
            int score = go.score();
            Integer isCorrect = go.isCorrect();

            // 每题作答落 answer_record（paper_id 关联本试卷）
            AnswerRecord ar = new AnswerRecord();
            ar.setUserId(userId);
            ar.setPaperId(record.getPaperId());
            ar.setQuestionId(q.getId());
            ar.setUserAnswer(userAnswer);
            ar.setIsCorrect(isCorrect);
            ar.setScore(score);
            ar.setAiComment(go.comment());
            ar.setDuration(perDuration);
            answerRecordMapper.insert(ar);

            // 错题本联动
            if (isCorrect != null) {
                if (isCorrect == 0) {
                    wrongQuestionService.collectWrong(userId, q.getId());
                } else {
                    wrongQuestionService.markMasteredIfCorrect(userId, q.getId());
                }
            }

            totalScore += full;
            obtained += score;
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
            detail.setUserAnswer(userAnswer);
            detail.setReferenceAnswer(q.getAnswer());
            detail.setAnalysis(q.getAnalysis());
            detail.setIsCorrect(isCorrect);
            detail.setScore(score);
            detail.setFullScore(full);
            detail.setAiComment(go.comment());
            details.add(detail);
        }

        int usedSeconds = record.getStartTime() == null ? 0
                : (int) Math.max(0, Duration.between(record.getStartTime(), submitTime).getSeconds());

        ExamRecord update = new ExamRecord();
        update.setId(record.getId());
        update.setTotalScore(obtained);
        update.setCorrectCount(correct);
        update.setWrongCount(wrong);
        update.setDuration(usedSeconds);
        update.setStatus(RECORD_SUBMITTED);
        update.setSubmitTime(submitTime);
        examRecordMapper.updateById(update);

        int total = details.size();
        ExamResultVO result = new ExamResultVO();
        result.setRecordId(record.getId());
        result.setObtainedScore(obtained);
        result.setTotalScore(totalScore);
        result.setCorrectCount(correct);
        result.setWrongCount(wrong);
        result.setUngradedCount(ungraded);
        result.setTotalCount(total);
        result.setAccuracy(total == 0 ? 0.0 : Math.round(correct * 1000.0 / total) / 10.0);
        result.setDuration(usedSeconds);
        result.setDetails(details);
        return result;
    }

    @Override
    public PageResult<ExamRecordVO> myExamRecords(int pageNum, int pageSize) {
        Long userId = currentUserId();
        int pn = pageNum < 1 ? 1 : pageNum;
        int ps = pageSize < 1 ? 10 : pageSize;
        Page<ExamRecord> page = examRecordMapper.selectPage(new Page<>(pn, ps),
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .orderByDesc(ExamRecord::getStartTime)
                        .orderByDesc(ExamRecord::getId));

        List<ExamRecord> records = page.getRecords();
        Map<Long, ExamPaper> paperMap = records.isEmpty() ? Collections.emptyMap()
                : examPaperMapper.selectBatchIds(records.stream().map(ExamRecord::getPaperId)
                        .distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(ExamPaper::getId, Function.identity()));

        List<ExamRecordVO> vos = records.stream().map(r -> {
            ExamRecordVO vo = new ExamRecordVO();
            vo.setId(r.getId());
            vo.setPaperId(r.getPaperId());
            vo.setTotalScore(r.getTotalScore());
            vo.setCorrectCount(r.getCorrectCount());
            vo.setWrongCount(r.getWrongCount());
            vo.setDuration(r.getDuration());
            vo.setStatus(r.getStatus());
            vo.setStartTime(r.getStartTime());
            vo.setSubmitTime(r.getSubmitTime());
            ExamPaper p = paperMap.get(r.getPaperId());
            if (p != null) {
                vo.setPaperTitle(p.getTitle());
                vo.setSubject(p.getSubject());
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), vos);
    }

    // ==================== 公共辅助 ====================

    private List<ExamPaperQuestion> listItems(Long paperId) {
        return examPaperQuestionMapper.selectList(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, paperId)
                .orderByAsc(ExamPaperQuestion::getSort)
                .orderByAsc(ExamPaperQuestion::getId));
    }

    private Map<Long, Question> loadQuestions(List<Long> ids) {
        List<Long> distinct = ids.stream().distinct().collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return questionMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
    }

    private ExamPaperVO toPaperVO(ExamPaper p) {
        ExamPaperVO vo = new ExamPaperVO();
        copyPaper(p, vo);
        return vo;
    }

    private void copyPaper(ExamPaper p, ExamPaperVO vo) {
        vo.setId(p.getId());
        vo.setTitle(p.getTitle());
        vo.setSubject(p.getSubject());
        vo.setDescription(p.getDescription());
        vo.setTotalScore(p.getTotalScore());
        vo.setQuestionCount(p.getQuestionCount());
        vo.setDuration(p.getDuration());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        vo.setUpdateTime(p.getUpdateTime());
    }

    private ExamPaper requirePaper(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "试卷ID不能为空");
        }
        ExamPaper paper = examPaperMapper.selectById(id);
        if (paper == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "试卷不存在");
        }
        return paper;
    }

    /** 归属校验：管理员放行，普通用户只能操作自己创建的试卷 */
    private void checkOwner(ExamPaper paper) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        boolean isAdmin = loginUser.getRole() != null && loginUser.getRole() == ROLE_ADMIN;
        boolean isOwner = paper.getUserId() != null && paper.getUserId().equals(loginUser.getUserId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人试卷");
        }
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
