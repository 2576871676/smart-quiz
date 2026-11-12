import request from '@/utils/request'

// 错题本
// WrongQuestionQueryDTO{pageNum,pageSize,subject,knowledgePoint,mastered,sortBy}
// → PageResult{records:WrongQuestionVO[]}
export function pageWrongQuestions(params) {
  return request.get('/wrong-question/page', { params })
}

// 错题统计 → WrongStatVO{totalCount,masteredCount,masteryRate,subjectStats[],weakPoints[]}
export function getWrongStatistics() {
  return request.get('/wrong-question/statistics')
}

// 标记某题已掌握
export function markMastered(questionId) {
  return request.put(`/wrong-question/${questionId}/master`)
}

// 从错题本移除
export function removeWrongQuestion(questionId) {
  return request.delete(`/wrong-question/${questionId}`)
}
