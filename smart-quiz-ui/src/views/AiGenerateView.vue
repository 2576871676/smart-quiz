<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 左侧：出题条件 -->
      <el-col :md="8" :xs="24">
        <el-card shadow="never">
          <template #header><span class="card-title">出题条件</span></template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="科目" prop="subject">
              <el-select v-model="form.subject" filterable allow-create placeholder="选择或输入" style="width: 100%">
                <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="知识点" prop="knowledgePoint">
              <el-input v-model="form.knowledgePoint" placeholder="如：HashMap 原理" />
            </el-form-item>
            <el-form-item label="题型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度" prop="difficulty">
              <el-radio-group v-model="form.difficulty">
                <el-radio-button v-for="d in DIFFICULTIES" :key="d.value" :value="d.value">
                  {{ d.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="数量" prop="count">
              <el-input-number v-model="form.count" :min="1" :max="10" />
            </el-form-item>
            <el-form-item label="每题分值">
              <el-input-number v-model="form.score" :min="1" :max="100" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="generating"
                style="width: 100%"
                @click="onGenerate"
              >
                生成题目（预览）
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="生成后先预览，满意再一键保存到题库"
          />
        </el-card>
      </el-col>

      <!-- 右侧：预览结果 -->
      <el-col :md="16" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="result-header">
              <span class="card-title">生成结果（{{ preview.length }} 题）</span>
              <el-button
                v-if="preview.length && !saved"
                type="success"
                :icon="Check"
                :loading="saving"
                @click="saveAll"
              >
                全部保存到题库
              </el-button>
              <el-tag v-if="saved" type="success" effect="dark">已入库</el-tag>
            </div>
          </template>

          <el-empty v-if="!preview.length && !generating" description="填写左侧条件后点击生成" />
          <div v-loading="generating" class="preview-list">
            <el-card
              v-for="(q, i) in preview"
              :key="i"
              shadow="hover"
              class="preview-item"
            >
              <div class="q-head">
                <el-tag size="small">{{ typeLabel(q.type) }}</el-tag>
                <el-tag size="small" :type="difficultyTag(q.difficulty)">
                  {{ difficultyLabel(q.difficulty) }}
                </el-tag>
                <el-tag size="small" effect="plain">{{ q.subject }}</el-tag>
                <span class="text-muted kp">{{ q.knowledgePoint }}</span>
                <span class="spacer"></span>
                <span class="text-muted">{{ q.score }} 分</span>
              </div>
              <div class="q-title">{{ i + 1 }}. {{ q.title }}</div>
              <div v-if="parseOptions(q.options).length" class="q-options">
                <div v-for="(o, oi) in parseOptions(q.options)" :key="oi">{{ o }}</div>
              </div>
              <div class="q-answer">
                <strong>答案：</strong>{{ q.answer }}
              </div>
              <div v-if="q.analysis" class="q-analysis text-muted">
                <strong>解析：</strong>{{ q.analysis }}
              </div>
            </el-card>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Check } from '@element-plus/icons-vue'
import { generateQuestions } from '@/api/ai'
import { createQuestion } from '@/api/question'
import {
  SUBJECTS,
  QUESTION_TYPES,
  DIFFICULTIES,
  typeLabel,
  difficultyLabel,
  difficultyTag,
  parseOptions
} from '@/utils/dict'

const formRef = ref()
const generating = ref(false)
const saving = ref(false)
const saved = ref(false)
const preview = ref([])

const form = reactive({
  subject: 'Java',
  knowledgePoint: '',
  type: 1,
  difficulty: 2,
  count: 3,
  score: 5,
  save: false
})

const rules = {
  subject: [{ required: true, message: '请选择科目', trigger: 'change' }],
  knowledgePoint: [{ required: true, message: '请输入知识点', trigger: 'blur' }]
}

async function onGenerate() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  generating.value = true
  saved.value = false
  preview.value = []
  try {
    const data = await generateQuestions({ ...form, save: false })
    preview.value = Array.isArray(data) ? data : data?.records || []
    if (!preview.value.length) ElMessage.warning('未生成题目，请调整条件重试')
  } finally {
    generating.value = false
  }
}

async function saveAll() {
  saving.value = true
  try {
    for (const q of preview.value) {
      await createQuestion({
        subject: q.subject,
        knowledgePoint: q.knowledgePoint,
        type: q.type,
        difficulty: q.difficulty,
        title: q.title,
        options: q.options,
        answer: q.answer,
        analysis: q.analysis,
        score: q.score,
        source: q.source || 'AI生成'
      })
    }
    saved.value = true
    ElMessage.success(`已保存 ${preview.value.length} 道题目到题库`)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.preview-list {
  min-height: 120px;
}
.preview-item {
  margin-bottom: 12px;
}
.q-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.q-head .kp {
  font-size: 12px;
}
.q-head .spacer {
  flex: 1;
}
.q-title {
  font-weight: 600;
  margin-bottom: 8px;
  line-height: 1.5;
}
.q-options {
  margin: 6px 0 6px 8px;
  color: #606266;
  line-height: 1.8;
}
.q-answer {
  color: #67c23a;
  margin-top: 6px;
}
.q-analysis {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
}
</style>
