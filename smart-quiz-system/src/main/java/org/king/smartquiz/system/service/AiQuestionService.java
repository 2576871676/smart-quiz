package org.king.smartquiz.system.service;

import org.king.smartquiz.system.dto.request.AiGenerateDTO;
import org.king.smartquiz.system.vo.QuestionVO;

import java.util.List;

/**
 * AI 智能出题服务：调用大模型生成题目、解析并按需入库。
 */
public interface AiQuestionService {

    /**
     * 根据入参生成题目。
     *
     * @param dto 出题要求
     * @return 生成的题目列表；dto.save=true 时已入库并带主键ID，否则 ID 为空仅供预览
     */
    List<QuestionVO> generate(AiGenerateDTO dto);
}
