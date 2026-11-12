import request from '@/utils/request'

// 试卷管理与试卷模式答题
// SaveExamPaperDTO{title,subject,description,duration,status,items:ExamPaperItemDTO[]{questionId,score}}

// 新建试卷 → 试卷ID
export function createPaper(data) {
  return request.post('/exam/paper', data)
}

// 修改试卷（覆盖题目清单）
export function updatePaper(id, data) {
  return request.put(`/exam/paper/${id}`, data)
}

// 删除试卷
export function deletePaper(id) {
  return request.delete(`/exam/paper/${id}`)
}

// 分页查询试卷
// ExamPaperQueryDTO{pageNum,pageSize,subject,status,keyword,onlyMine} → PageResult{records:ExamPaperVO[]}
export function pagePapers(params) {
  return request.get('/exam/paper/page', { params })
}

// 试卷详情（含答案与解析）→ ExamPaperDetailVO{...ExamPaperVO,items:ExamPaperQuestionVO[]}
export function getPaperDetail(id) {
  return request.get(`/exam/paper/${id}`)
}

// 发布试卷
export function publishPaper(id) {
  return request.put(`/exam/paper/${id}/publish`)
}

// 开考 → ExamSessionVO{recordId,paperId,title,subject,duration,startTime,questions:ExamQuestionVO[]}
export function startExam(paperId) {
  return request.post(`/exam/start/${paperId}`)
}

// 交卷批改
// SubmitExamDTO{recordId,answers:AnswerItemDTO[]{questionId,userAnswer,duration}} → ExamResultVO
export function submitExam(data) {
  return request.post('/exam/submit', data)
}

// 我的考试记录分页 → PageResult{records:ExamRecordVO[]}
export function pageExamRecords(params) {
  return request.get('/exam/records', { params })
}
