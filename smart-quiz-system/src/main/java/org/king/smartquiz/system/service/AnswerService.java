package org.king.smartquiz.system.service;

import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.system.dto.request.SubmitAnswerDTO;
import org.king.smartquiz.system.vo.AnswerRecordVO;
import org.king.smartquiz.system.vo.AnswerResultVO;

/**
 * 在线答题与自动批改服务。
 */
public interface AnswerService {

    /**
     * 提交答卷并自动批改：客观题本地判分，主观题 AI 评分，逐题写入答题记录。
     *
     * @return 批改汇总（含每题明细）
     */
    AnswerResultVO submit(SubmitAnswerDTO dto);

    /** 分页查询当前用户的答题记录 */
    PageResult<AnswerRecordVO> myRecords(int pageNum, int pageSize);
}
