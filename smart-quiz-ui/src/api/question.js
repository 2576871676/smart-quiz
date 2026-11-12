import request from '@/utils/request'

// 题库管理
// QuestionDTO{subject,knowledgePoint,type,difficulty,title,options,answer,analysis,score,source}
export function createQuestion(data) {
  return request.post('/question', data)
}

export function updateQuestion(id, data) {
  return request.put(`/question/${id}`, data)
}

export function deleteQuestion(id) {
  return request.delete(`/question/${id}`)
}

export function getQuestion(id) {
  return request.get(`/question/${id}`)
}

// QuestionQueryDTO{pageNum,pageSize,subject,type,difficulty,knowledgePoint,keyword,onlyMine}
// 查询参数绑定 → PageResult{total,pageNum,pageSize,records:QuestionVO[]}
export function pageQuestions(params) {
  return request.get('/question/page', { params })
}
