<template>
  <div class="page-container">
    <!-- 步骤条 -->
    <el-steps :active="step" align-center finish-status="success" class="mb-12">
      <el-step title="选择题目" />
      <el-step title="作答" />
      <el-step title="批改结果" />
    </el-steps>

    <!-- Step 0: 选题 -->
    <el-card v-if="step === 0" shadow="never">
      <template #header><span class="card-title">组卷条件</span></template>
      <el-form :model="pick" label-width="90px" style="max-width: 560px">
        <el-form-item label="科目">
          <el-select v-model="pick.subject" filterable allow-create clearable placeholder="不限" style="width: 100%">
            <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="pick.type" clearable placeholder="不限" style="width: 100%">
            <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="pick.difficulty" clearable placeholder="不限" style="width: 100%">
            <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目数量">
          <el-input-number v-model="pick.count" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="范围">
          <el-checkbox v-model="pick.onlyMine" border>仅我创建的题目</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="EditPen" :loading="loadingQ" @click="startPractice">
            开始答题
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="题库需有足够题目；可先到「题库管理」新增或用「AI 出题」生成"
      />
    </el-card>

    <!-- Step 1: 作答 -->
    <el-card v-else-if="step === 1" shadow="never">
      <template #header>
        <div class="answer-header">
          <span class="card-title">共 {{ questions.length }} 题，已答 {{ answeredCount }} 题</span>
          <el-button type="primary" :loading="submitting" @click="onSubmit">交卷批改</el-button>
        </div>
      </template>

      <div v-for="(q, i) in questions" :key="q.id" class="question-block">
        <div class="q-title">
          <span class="q-no">{{ i + 1 }}</span>
          <el-tag size="small">{{ typeLabel(q.type) }}</el-tag>
          <el-tag size="small" :type="difficultyTag(q.difficulty)">{{ difficultyLabel(q.difficulty) }}</el-tag>
          <span class="text-muted">（{{ q.score }} 分）</span>
          <span class="q-text">{{ q.title }}</span>
        </div>

        <!-- 单选 -->
        <el-radio-group v-if="q.type === 1" v-model="answers[q.id]" class="opt-group">
          <el-radio v-for="(o, oi) in parseOptions(q.options)" :key="oi" :value="optionKey(o, oi)" class="opt-line">
            {{ o }}
          </el-radio>
        </el-radio-group>

        <!-- 多选 -->
        <el-checkbox-group v-else-if="q.type === 2" v-model="multiAnswers[q.id]" class="opt-group">
          <el-checkbox v-for="(o, oi) in parseOptions(q.options)" :key="oi" :value="optionKey(o, oi)" class="opt-line">
            {{ o }}
          </el-checkbox>
        </el-checkbox-group>

        <!-- 填空 -->
        <el-input
          v-else-if="q.type === 3"
          v-model="answers[q.id]"
          placeholder="多个空请用 | 分隔，如 O(n)|O(1)"
          class="answer-input"
        />

        <!-- 简答 -->
        <el-input
          v-else
          v-model="answers[q.id]"
          type="textarea"
          :rows="4"
          placeholder="请输入你的解答（AI 批改）"
          class="answer-input"
        />
      </div>
    </el-card>

    <!-- Step 2: 结果 -->
    <div v-else>
      <el-card shadow="never" class="mb-12">
        <div class="result-summary">
          <el-statistic title="得分" :value="result.obtainedScore || 0">
            <template #suffix>/ {{ result.totalScore || 0 }}</template>
          </el-statistic>
          <el-statistic title="正确率" :value="result.accuracy || 0" suffix="%" />
          <el-statistic title="答对" :value="result.correctCount || 0" />
          <el-statistic title="答错" :value="result.wrongCount || 0" />
          <el-statistic title="未判分" :value="result.ungradedCount || 0" />
        </div>
        <div class="result-actions">
          <el-button @click="backToPick">再练一组</el-button>
          <el-button type="primary" @click="$router.push('/wrong-book')">查看错题本</el-button>
        </div>
      </el-card>

      <el-card v-for="(d, i) in result.details || []" :key="i" shadow="never" class="detail-card">
        <div class="d-head">
          <span class="q-no">{{ i + 1 }}</span>
          <el-tag size="small">{{ typeLabel(d.type) }}</el-tag>
          <el-tag
            size="small"
            :type="d.isCorrect === 1 ? 'success' : d.isCorrect === 0 ? 'danger' : 'info'"
          >
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
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { pageQuestions } from '@/api/question'
import { submitAnswers } from '@/api/answer'
import {
  SUBJECTS,
  QUESTION_TYPES,
  DIFFICULTIES,
  typeLabel,
  difficultyLabel,
  difficultyTag,
  parseOptions,
  optionKey
} from '@/utils/dict'

const step = ref(0)
const loadingQ = ref(false)
const submitting = ref(false)
const questions = ref([])
const answers = reactive({}) // questionId → string
const multiAnswers = reactive({}) // questionId → string[]
const result = ref({})
let startTime = 0

const pick = reactive({ subject: '', type: null, difficulty: null, count: 5, onlyMine: false })

const answeredCount = computed(() =>
  questions.value.filter((q) => {
    if (q.type === 2) return (multiAnswers[q.id] || []).length > 0
    return !!(answers[q.id] && String(answers[q.id]).trim())
  }).length
)

async function startPractice() {
  loadingQ.value = true
  try {
    const params = { pageNum: 1, pageSize: pick.count }
    if (pick.subject) params.subject = pick.subject
    if (pick.type) params.type = pick.type
    if (pick.difficulty) params.difficulty = pick.difficulty
    if (pick.onlyMine) params.onlyMine = true
    const page = await pageQuestions(params)
    const records = page.records || []
    if (!records.length) {
      ElMessage.warning('没有符合条件的题目，请调整条件或先添加题目')
      return
    }
    questions.value = records
    Object.keys(answers).forEach((k) => delete answers[k])
    Object.keys(multiAnswers).forEach((k) => delete multiAnswers[k])
    records.forEach((q) => {
      answers[q.id] = ''
      multiAnswers[q.id] = []
    })
    startTime = Date.now()
    step.value = 1
  } finally {
    loadingQ.value = false
  }
}

async function onSubmit() {
  if (!answeredCount.value) {
    ElMessage.warning('请至少作答一题')
    return
  }
  const duration = Math.round((Date.now() - startTime) / 1000 / Math.max(1, questions.value.length))
  const payload = questions.value.map((q) => {
    let userAnswer
    if (q.type === 2) {
      userAnswer = (multiAnswers[q.id] || []).slice().sort().join('')
    } else {
      userAnswer = answers[q.id] || ''
    }
    return { questionId: q.id, userAnswer, duration }
  })
  submitting.value = true
  try {
    result.value = await submitAnswers({ answers: payload })
    step.value = 2
    ElMessage.success('批改完成')
  } finally {
    submitting.value = false
  }
}

function backToPick() {
  step.value = 0
  result.value = {}
  questions.value = []
}
</script>

<style scoped>
.answer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  gap: 48px;
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
