<template>
  <div class="page-container">
    <!-- 加载中 -->
    <el-card v-if="phase === 'loading'" shadow="never">
      <el-skeleton :rows="6" animated />
    </el-card>

    <!-- 作答 -->
    <template v-else-if="phase === 'exam'">
      <el-card shadow="never" class="mb-12 sticky-bar">
        <div class="exam-bar">
          <div class="exam-info">
            <span class="card-title">{{ session.title }}</span>
            <el-tag size="small" effect="plain">{{ session.subject || '综合' }}</el-tag>
            <span class="text-muted">共 {{ questions.length }} 题 · 已答 {{ answeredCount }} 题</span>
          </div>
          <div class="exam-right">
            <el-tag :type="remaining < 60 ? 'danger' : 'warning'" effect="dark" size="large" class="timer">
              <el-icon><Clock /></el-icon> {{ countdownText }}
            </el-tag>
            <el-button type="primary" :loading="submitting" @click="onSubmit">交卷</el-button>
          </div>
        </div>
        <el-progress
          :percentage="answeredPercent"
          :stroke-width="6"
          :show-text="false"
          style="margin-top: 8px"
        />
      </el-card>

      <el-card shadow="never">
        <div v-for="(q, i) in questions" :key="q.questionId" class="question-block">
          <div class="q-title">
            <span class="q-no">{{ i + 1 }}</span>
            <el-tag size="small">{{ typeLabel(q.type) }}</el-tag>
            <el-tag size="small" :type="difficultyTag(q.difficulty)">{{ difficultyLabel(q.difficulty) }}</el-tag>
            <span class="text-muted">（{{ q.score }} 分）</span>
            <span class="q-text">{{ q.title }}</span>
          </div>

          <!-- 单选 -->
          <el-radio-group v-if="q.type === 1" v-model="answers[q.questionId]" class="opt-group">
            <el-radio v-for="(o, oi) in parseOptions(q.options)" :key="oi" :value="optionKey(o, oi)" class="opt-line">
              {{ o }}
            </el-radio>
          </el-radio-group>

          <!-- 多选 -->
          <el-checkbox-group v-else-if="q.type === 2" v-model="multiAnswers[q.questionId]" class="opt-group">
            <el-checkbox v-for="(o, oi) in parseOptions(q.options)" :key="oi" :value="optionKey(o, oi)" class="opt-line">
              {{ o }}
            </el-checkbox>
          </el-checkbox-group>

          <!-- 填空 -->
          <el-input
            v-else-if="q.type === 3"
            v-model="answers[q.questionId]"
            placeholder="多个空请用 | 分隔，如 O(n)|O(1)"
            class="answer-input"
          />

          <!-- 简答 -->
          <el-input
            v-else
            v-model="answers[q.questionId]"
            type="textarea"
            :rows="4"
            placeholder="请输入你的解答（AI 批改）"
            class="answer-input"
          />
        </div>
      </el-card>
    </template>

    <!-- 结果 -->
    <template v-else>
      <el-card shadow="never" class="mb-12">
        <div class="result-summary">
          <el-statistic title="得分" :value="result.obtainedScore || 0">
            <template #suffix>/ {{ result.totalScore || 0 }}</template>
          </el-statistic>
          <el-statistic title="正确率" :value="result.accuracy || 0" suffix="%" />
          <el-statistic title="答对" :value="result.correctCount || 0" />
          <el-statistic title="答错" :value="result.wrongCount || 0" />
          <el-statistic title="未判分" :value="result.ungradedCount || 0" />
          <el-statistic title="用时(秒)" :value="result.duration || 0" />
        </div>
        <div class="result-actions">
          <el-button @click="backToCenter">返回考试中心</el-button>
          <el-button type="primary" @click="$router.push('/wrong-book')">查看错题本</el-button>
        </div>
      </el-card>

      <el-card v-for="(d, i) in result.details || []" :key="i" shadow="never" class="detail-card">
        <div class="d-head">
          <span class="q-no">{{ i + 1 }}</span>
          <el-tag size="small">{{ typeLabel(d.type) }}</el-tag>
          <el-tag size="small" :type="d.isCorrect === 1 ? 'success' : d.isCorrect === 0 ? 'danger' : 'info'">
            {{ d.isCorrect === 1 ? '正确' : d.isCorrect === 0 ? '错误' : '未判分' }}
          </el-tag>
          <span class="text-muted">{{ d.score }} / {{ d.fullScore }} 分</span>
        </div>
        <div class="d-title">{{ d.title }}</div>
        <div v-if="parseOptions(d.options).length" class="d-options">
          <div v-for="(o, oi) in parseOptions(d.options)" :key="oi">{{ o }}</div>
        </div>
        <el-descriptions :column="1" border size="small" class="mt-12">
          <el-descriptions-item label="你的答案">
            <span :class="d.isCorrect === 0 ? 'wrong-text' : ''">{{ d.userAnswer || '（未作答）' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="参考答案">{{ d.referenceAnswer }}</el-descriptions-item>
          <el-descriptions-item v-if="d.analysis" label="解析">{{ d.analysis }}</el-descriptions-item>
          <el-descriptions-item v-if="d.aiComment" label="AI 评语">{{ d.aiComment }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onActivated, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'
import { startExam, submitExam } from '@/api/exam'
import {
  typeLabel, difficultyLabel, difficultyTag, parseOptions, optionKey
} from '@/utils/dict'

const route = useRoute()
const router = useRouter()

const phase = ref('loading') // loading | exam | result
const submitting = ref(false)
const session = ref({})
const questions = ref([])
const answers = reactive({}) // questionId → string
const multiAnswers = reactive({}) // questionId → string[]
const result = ref({})

const remaining = ref(0) // 剩余秒数
let timer = null
let startTs = 0
let initing = false // 同步守卫：keep-alive 首次挂载 onMounted+onActivated 会同时触发，防止重复开考
const loadedPaperId = ref(null)

const answeredCount = computed(() =>
  questions.value.filter((q) => {
    if (q.type === 2) return (multiAnswers[q.questionId] || []).length > 0
    return !!(answers[q.questionId] && String(answers[q.questionId]).trim())
  }).length
)
const answeredPercent = computed(() =>
  questions.value.length ? Math.round((answeredCount.value / questions.value.length) * 100) : 0
)
const countdownText = computed(() => {
  const r = Math.max(0, remaining.value)
  const m = String(Math.floor(r / 60)).padStart(2, '0')
  const s = String(r % 60).padStart(2, '0')
  return `${m}:${s}`
})

async function init() {
  if (initing) return
  const paperId = route.query.paperId
  if (!paperId) {
    ElMessage.error('缺少试卷参数')
    router.replace('/exam-center')
    return
  }
  initing = true
  loadedPaperId.value = paperId
  clearTimer()
  phase.value = 'loading'
  result.value = {}
  Object.keys(answers).forEach((k) => delete answers[k])
  Object.keys(multiAnswers).forEach((k) => delete multiAnswers[k])
  try {
    const data = await startExam(paperId)
    session.value = data
    questions.value = data.questions || []
    questions.value.forEach((q) => {
      answers[q.questionId] = ''
      multiAnswers[q.questionId] = []
    })
    remaining.value = (data.duration || 60) * 60
    startTs = Date.now()
    phase.value = 'exam'
    startTimer()
  } catch {
    phase.value = 'result'
  } finally {
    initing = false
  }
}

function startTimer() {
  clearTimer()
  timer = setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) {
      remaining.value = 0
      clearTimer()
      autoSubmit()
    }
  }, 1000)
}

function clearTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function buildPayload() {
  const elapsed = Math.round((Date.now() - startTs) / 1000)
  const per = questions.value.length ? Math.max(1, Math.round(elapsed / questions.value.length)) : 0
  return questions.value.map((q) => {
    let userAnswer
    if (q.type === 2) {
      userAnswer = (multiAnswers[q.questionId] || []).slice().sort().join('')
    } else {
      userAnswer = answers[q.questionId] || ''
    }
    return { questionId: q.questionId, userAnswer, duration: per }
  })
}

async function doSubmit() {
  clearTimer()
  submitting.value = true
  try {
    result.value = await submitExam({ recordId: session.value.recordId, answers: buildPayload() })
    phase.value = 'result'
    ElMessage.success('交卷成功，已完成批改')
  } finally {
    submitting.value = false
  }
}

async function onSubmit() {
  const unanswered = questions.value.length - answeredCount.value
  const tip = unanswered > 0 ? `还有 ${unanswered} 题未作答，确定交卷吗？` : '确定交卷并批改吗？'
  try {
    await ElMessageBox.confirm(tip, '交卷确认', { type: 'warning' })
  } catch {
    return
  }
  await doSubmit()
}

async function autoSubmit() {
  ElMessage.warning('考试时间已到，系统自动交卷')
  await doSubmit()
}

function backToCenter() {
  router.push('/exam-center')
}

onMounted(init)
// keep-alive 缓存：作答中切走再回来则继续；换试卷或已出结果则按新一次考试重新开考
onActivated(() => {
  const pid = String(route.query.paperId || '')
  if (pid !== String(loadedPaperId.value || '') || phase.value !== 'exam') init()
})
onUnmounted(clearTimer)
</script>

<style scoped>
.sticky-bar {
  position: sticky;
  top: 0;
  z-index: 10;
}
.exam-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.exam-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.exam-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.timer {
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}
.question-block {
  padding: 16px 0;
  border-bottom: 1px dashed #e6e8eb;
}
.question-block:last-child {
  border-bottom: none;
}
.q-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  line-height: 1.6;
}
.q-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}
.q-text {
  font-weight: 600;
}
.opt-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-left: 32px;
}
.opt-line {
  height: auto;
  margin-right: 0;
}
.answer-input {
  margin-left: 32px;
  max-width: 640px;
}
.result-summary {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
  justify-content: space-around;
  padding: 8px 0;
}
.result-actions {
  text-align: center;
  margin-top: 16px;
}
.detail-card {
  margin-bottom: 12px;
}
.d-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.d-title {
  font-weight: 600;
  line-height: 1.6;
}
.d-options {
  margin: 8px 0 0 32px;
  color: #606266;
  line-height: 1.8;
}
.wrong-text {
  color: #f56c6c;
  font-weight: 600;
}
</style>
