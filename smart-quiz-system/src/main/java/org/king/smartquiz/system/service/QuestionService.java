package org.king.smartquiz.system.service;

import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.system.dto.request.QuestionDTO;
import org.king.smartquiz.system.dto.request.QuestionQueryDTO;
import org.king.smartquiz.system.vo.QuestionVO;

/**
 * 题目管理服务：CRUD + 分页查询。
 */
public interface QuestionService {

    /** 新增题目，返回主键ID */
    Long addQuestion(QuestionDTO dto);

    /** 修改题目（校验归属：非管理员不可改他人题目） */
    void updateQuestion(Long id, QuestionDTO dto);

    /** 删除题目（逻辑删除，校验归属） */
    void deleteQuestion(Long id);

    /** 查看题目详情 */
    QuestionVO getQuestionDetail(Long id);

    /** 分页查询题目 */
    PageResult<QuestionVO> pageQuery(QuestionQueryDTO query);
}
