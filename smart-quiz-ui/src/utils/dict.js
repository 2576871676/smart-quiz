// 全局字典：题型 / 难度 / 科目，供各页面下拉与展示复用
// 权威定义（见后端 Question.java）：题型 1-单选 2-多选 3-填空 4-简答（无判断题）

export const QUESTION_TYPES = [
  { value: 1, label: '单选题' },
  { value: 2, label: '多选题' },
  { value: 3, label: '填空题' },
  { value: 4, label: '简答题' }
]

// 难度 difficulty：1-易 2-中 3-难
export const DIFFICULTIES = [
  { value: 1, label: '简单' },
  { value: 2, label: '中等' },
  { value: 3, label: '困难' }
]

// 常见科目（可自由输入，这里给候选）
export const SUBJECTS = ['Java', '数据结构', '操作系统', '计算机网络', '数据库', '算法', 'Spring', '英语']

// 有选项的题型：单选 / 多选（填空、简答无选项）
export function isChoiceType(type) {
  return Number(type) === 1 || Number(type) === 2
}

// 是否主观题（AI 批改）：简答
export function isSubjectiveType(type) {
  return Number(type) === 4
}

export function typeLabel(v) {
  return QUESTION_TYPES.find((t) => t.value === Number(v))?.label || '未知'
}

export function difficultyLabel(v) {
  return DIFFICULTIES.find((d) => d.value === Number(v))?.label || '未知'
}

// 难度对应的 Element tag 类型
export function difficultyTag(v) {
  return { 1: 'success', 2: 'warning', 3: 'danger' }[Number(v)] || 'info'
}

// 解析 options（后端存的可能是 JSON 字符串或数组）
// AI 生成/前端保存统一为 [{key:'A',value:'..'}]
export function parseOptions(options) {
  if (!options) return []
  if (Array.isArray(options)) {
    return options.map((o) => normalizeOption(o)).filter(Boolean)
  }
  try {
    const parsed = JSON.parse(options)
    if (Array.isArray(parsed)) return parsed.map((o) => normalizeOption(o)).filter(Boolean)
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed).map(([k, v]) => `${k}. ${v}`)
    }
  } catch {
    // 非 JSON：按换行拆分
  }
  return String(options)
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

function normalizeOption(o) {
  if (typeof o === 'string') return o
  if (o && typeof o === 'object') {
    const key = o.key ?? o.label ?? ''
    const val = o.value ?? o.text ?? o.content ?? ''
    return key ? `${key}. ${val}` : String(val)
  }
  return o == null ? '' : String(o)
}

// 从 "A. 内容" 形式的展示串里取出选项字母
export function optionKey(displayStr, index) {
  const m = /^([A-H])\./.exec(displayStr)
  if (m) return m[1]
  return String.fromCharCode(65 + index)
}
