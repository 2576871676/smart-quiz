package org.king.smartquiz.system.service;

import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.system.dto.request.WrongQuestionQueryDTO;
import org.king.smartquiz.system.vo.WrongQuestionVO;
import org.king.smartquiz.system.vo.WrongStatVO;

/**
 * 错题本服务：答错自动收录、复习列表、统计、掌握标记、移除。
 */
public interface WrongQuestionService {

    /** 答错时收录/累加错题（供答题模块调用） */
    void collectWrong(Long userId, Long questionId);

    /** 答对时若该题在错题本且未掌握，则标记为已掌握（供答题模块调用，实现「重做答对→掌握」） */
    void markMasteredIfCorrect(Long userId, Long questionId);

    /** 分页查询当前用户错题（支持科目/知识点/掌握状态筛选） */
    PageResult<WrongQuestionVO> page(WrongQuestionQueryDTO query);

    /** 当前用户错题统计 */
    WrongStatVO statistics();

    /** 手动标记某错题为已掌握 */
    void markMastered(Long questionId);

    /** 从错题本移除某题 */
    void remove(Long questionId);
}
