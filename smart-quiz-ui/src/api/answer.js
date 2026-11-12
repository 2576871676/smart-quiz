import request from '@/utils/request'

// 答题与批改
// SubmitAnswerDTO{answers:AnswerItemDTO[]{questionId,userAnswer,duration}}
// → AnswerResultVO{obtainedScore,totalScore,correctCount,wrongCount,ungradedCount,totalCount,accuracy,details:AnswerDetailVO[]}
export function submitAnswers(data) {
  return request.post('/answer/submit', data)
}

// 历史答题记录分页 → PageResult{records:AnswerRecordVO[]}
export function pageRecords(params) {
  return request.get('/answer/records', { params })
}
