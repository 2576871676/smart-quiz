package org.king.smartquiz.system.service;

import org.king.smartquiz.common.core.PageResult;
import org.king.smartquiz.system.dto.request.ExamPaperQueryDTO;
import org.king.smartquiz.system.dto.request.SaveExamPaperDTO;
import org.king.smartquiz.system.dto.request.SubmitExamDTO;
import org.king.smartquiz.system.vo.ExamPaperDetailVO;
import org.king.smartquiz.system.vo.ExamPaperVO;
import org.king.smartquiz.system.vo.ExamRecordVO;
import org.king.smartquiz.system.vo.ExamResultVO;
import org.king.smartquiz.system.vo.ExamSessionVO;

/**
 * 试卷管理与试卷模式答题服务。
 * 判分统一委托 {@link QuestionGrader}，与练习模式共用同一判分口径；
 * 每题作答明细落 answer_record（paper_id 关联试卷），会话汇总落 exam_record。
 */
public interface ExamService {

    /** 新建试卷（含题目清单），返回试卷ID */
    Long createPaper(SaveExamPaperDTO dto);

    /** 更新试卷（覆盖题目清单） */
    void updatePaper(Long id, SaveExamPaperDTO dto);

    /** 删除试卷（逻辑删除试卷 + 清理题目关系） */
    void deletePaper(Long id);

    /** 分页查询试卷 */
    PageResult<ExamPaperVO> pagePapers(ExamPaperQueryDTO query);

    /** 试卷详情（管理端，含答案与解析） */
    ExamPaperDetailVO getPaperDetail(Long id);

    /** 发布试卷（草稿 -> 已发布） */
    void publishPaper(Long id);

    /** 开考：创建考试会话并返回题目（不含答案） */
    ExamSessionVO startExam(Long paperId);

    /** 交卷：批改、落库、汇总，返回成绩与明细 */
    ExamResultVO submitExam(SubmitExamDTO dto);

    /** 我的考试记录（分页） */
    PageResult<ExamRecordVO> myExamRecords(int pageNum, int pageSize);
}
