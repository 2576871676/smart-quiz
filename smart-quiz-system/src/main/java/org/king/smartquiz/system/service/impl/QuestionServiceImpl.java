package org.king.smartquiz.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.framework.security.LoginUser;
import org.king.smartquiz.framework.security.UserContext;
import org.king.smartquiz.system.dto.request.QuestionDTO;
import org.king.smartquiz.system.dto.request.QuestionQueryDTO;
import org.king.smartquiz.system.entity.Question;
import org.king.smartquiz.system.mapper.QuestionMapper;
import org.king.smartquiz.system.service.QuestionService;
import org.king.smartquiz.system.vo.QuestionVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目管理服务实现。
 */
@Service
public class QuestionServiceImpl implements QuestionService {

    private static final int ROLE_ADMIN = 1;

    private final QuestionMapper questionMapper;

    public QuestionServiceImpl(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @Override
    public Long addQuestion(QuestionDTO dto) {
        Long userId = currentUserId();
        Question question = new Question();
        BeanUtil.copyProperties(dto, question);
        question.setUserId(userId);
        if (question.getScore() == null) {
            question.setScore(5);
        }
        if (question.getSource() == null) {
            question.setSource(2);
        }
        questionMapper.insert(question);
        return question.getId();
    }

    @Override
    public void updateQuestion(Long id, QuestionDTO dto) {
        Question existing = requireQuestion(id);
        checkOwner(existing);

        Question update = new Question();
        // 仅覆盖传入的非空字段，避免把未传字段清成 null
        BeanUtil.copyProperties(dto, update, "userId");
        update.setId(id);
        questionMapper.updateById(update);
    }

    @Override
    public void deleteQuestion(Long id) {
        Question existing = requireQuestion(id);
        checkOwner(existing);
        questionMapper.deleteById(id);
    }

    @Override
    public QuestionVO getQuestionDetail(Long id) {
        Question question = requireQuestion(id);
        QuestionVO vo = new QuestionVO();
        BeanUtil.copyProperties(question, vo);
        return vo;
    }

    @Override
    public PageResult<QuestionVO> pageQuery(QuestionQueryDTO query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(query.getSubject()), Question::getSubject, query.getSubject())
                .eq(query.getType() != null, Question::getType, query.getType())
                .eq(query.getDifficulty() != null, Question::getDifficulty, query.getDifficulty())
                .eq(StrUtil.isNotBlank(query.getKnowledgePoint()), Question::getKnowledgePoint, query.getKnowledgePoint())
                .like(StrUtil.isNotBlank(query.getKeyword()), Question::getTitle, query.getKeyword())
                .eq(Boolean.TRUE.equals(query.getOnlyMine()), Question::getUserId, currentUserId())
                .orderByDesc(Question::getCreateTime);

        Page<Question> page = questionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<QuestionVO> records = page.getRecords().stream().map(q -> {
            QuestionVO vo = new QuestionVO();
            BeanUtil.copyProperties(q, vo);
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /** 取题目，不存在抛 404 */
    private Question requireQuestion(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "题目ID不能为空");
        }
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    /** 归属校验：管理员放行，普通用户只能操作自己创建的题目 */
    private void checkOwner(Question question) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        boolean isAdmin = loginUser.getRole() != null && loginUser.getRole() == ROLE_ADMIN;
        boolean isOwner = question.getUserId() != null && question.getUserId().equals(loginUser.getUserId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人题目");
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
