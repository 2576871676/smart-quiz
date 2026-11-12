import request from '@/utils/request'

// AI 出题：save=true 入库，否则仅预览
// AiGenerateDTO{subject,knowledgePoint,type,difficulty,count,score,save}
// 返回 QuestionVO[]（预览或已入库）
export function generateQuestions(data) {
  return request.post('/ai/generate', data)
}
