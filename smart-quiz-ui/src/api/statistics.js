import request from '@/utils/request'

// 学习统计分析
export function getOverview() {
  // → StudyOverviewVO
  return request.get('/statistics/overview')
}

export function getAccuracy() {
  // → AccuracyStatVO{byType,byDifficulty,bySubject: DimensionStatVO[]}
  return request.get('/statistics/accuracy')
}

export function getTrend(days = 7) {
  // → TrendPointVO[]{date,total,correct,...}
  return request.get('/statistics/trend', { params: { days } })
}

export function getWeakPoints(limit = 5) {
  // → WeakPointVO[]
  return request.get('/statistics/weak-points', { params: { limit } })
}

export function getAiAdvice() {
  // → String（纯文本学习建议）
  return request.get('/statistics/advice')
}
