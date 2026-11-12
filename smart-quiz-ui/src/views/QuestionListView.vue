<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-card shadow="never" class="mb-12">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索题干关键字"
          clearable
          style="width: 220px"
          :prefix-icon="Search"
          @keyup.enter="reload"
        />
        <el-select v-model="query.subject" placeholder="科目" clearable style="width: 140px">
          <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="query.type" placeholder="题型" clearable style="width: 120px">
          <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 120px">
          <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
        <el-checkbox v-model="query.onlyMine" border>只看我的</el-checkbox>
        <div class="spacer"></div>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate">新增题目</el-button>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="subject" label="科目" width="110" />
        <el-table-column prop="knowledgePoint" label="知识点" width="140" show-overflow-tooltip />
        <el-table-column label="题型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyTag(row.difficulty)">
              {{ difficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="题干" min-width="240" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="70" align="center" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">查看</el-button>
            <el-button link type="warning" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无题目，点击「新增题目」或用 AI 出题" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="reload"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑题目' : '新增题目'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="科目" prop="subject">
              <el-select v-model="form.subject" filterable allow-create placeholder="选择或输入" style="width: 100%">
                <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="知识点" prop="knowledgePoint">
              <el-input v-model="form.knowledgePoint" placeholder="如：时间复杂度" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="题型" prop="type">
              <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
                <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="form.difficulty" style="width: 100%">
                <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分值" prop="score">
              <el-input-number v-model="form.score" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="题干" prop="title">
          <el-input v-model="form.title" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>

        <!-- 选项：仅单选/多选 -->
        <el-form-item v-if="hasOptions" label="选项">
          <div class="opt-editor">
            <div v-for="(opt, idx) in optionList" :key="idx" class="opt-row">
              <span class="opt-key">{{ optKeys[idx] }}.</span>
              <el-input v-model="optionList[idx]" placeholder="选项内容" />
              <el-button link type="danger" :icon="Delete" @click="optionList.splice(idx, 1)" />
            </div>
            <el-button link type="primary" :icon="Plus" @click="optionList.push('')">
              添加选项
            </el-button>
            <span class="text-muted tip">保存为 JSON：[{&quot;key&quot;:&quot;A&quot;,&quot;value&quot;:&quot;...&quot;}]</span>
          </div>
        </el-form-item>

        <el-form-item label="参考答案" prop="answer">
          <el-input
            v-model="form.answer"
            :type="isSubjective ? 'textarea' : 'text'"
            :rows="2"
            :placeholder="answerPlaceholder"
          />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="选填，解题思路" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情 -->
    <el-dialog v-model="detailVisible" title="题目详情" width="600px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="科目">{{ detail.subject }}</el-descriptions-item>
        <el-descriptions-item label="知识点">{{ detail.knowledgePoint }}</el-descriptions-item>
        <el-descriptions-item label="题型">{{ typeLabel(detail.type) }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ difficultyLabel(detail.difficulty) }}</el-descriptions-item>
        <el-descriptions-item label="分值">{{ detail.score }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.source || '—' }}</el-descriptions-item>
        <el-descriptions-item label="题干" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="选项" :span="2">
          <div v-for="(o, i) in parseOptions(detail.options)" :key="i">{{ o }}</div>
          <span v-if="!parseOptions(detail.options).length">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="参考答案" :span="2">{{ detail.answer }}</el-descriptions-item>
        <el-descriptions-item label="解析" :span="2">{{ detail.analysis || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, View } from '@element-plus/icons-vue'
import {
  pageQuestions,
  createQuestion,
  updateQuestion,
  deleteQuestion
} from '@/api/question'
import {
  SUBJECTS,
  QUESTION_TYPES,
  DIFFICULTIES,
  typeLabel,
  difficultyLabel,
  difficultyTag,
  parseOptions,
  isChoiceType,
  isSubjectiveType
} from '@/utils/dict'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  subject: '',
  type: null,
  difficulty: null,
  keyword: '',
  onlyMine: false
})

const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref()
const detail = ref(null)
const optionList = ref([])
const optKeys = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']

const emptyForm = () => ({
  subject: '',
  knowledgePoint: '',
  type: 1,
  difficulty: 2,
  title: '',
  options: '',
  answer: '',
  analysis: '',
  score: 5,
  source: '手动录入'
})
const form = reactive(emptyForm())

const rules = {
  subject: [{ required: true, message: '请选择科目', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  title: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入参考答案', trigger: 'blur' }]
}

const hasOptions = computed(() => isChoiceType(form.type))
const isSubjective = computed(() => isSubjectiveType(form.type))
const answerPlaceholder = computed(() => {
  switch (Number(form.type)) {
    case 1:
      return '单选填正确项字母，如 A'
    case 2:
      return '多选填字母组合，如 ABD'
    case 3:
      return '填空，多个空用 | 分隔，如 O(n)|O(1)'
    default:
      return '简答参考答案要点'
  }
})

function onTypeChange() {
  if (isChoiceType(form.type) && optionList.value.length === 0) {
    optionList.value = ['', '', '', '']
  }
}

function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    subject: '',
    type: null,
    difficulty: null,
    keyword: '',
    onlyMine: false
  })
  reload()
}

function reload() {
  query.pageNum = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    // 清理空值，避免后端类型转换报错
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const page = await pageQuestions(params)
    list.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, emptyForm())
  optionList.value = ['', '', '', '']
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, emptyForm(), row)
  // 反解析 options 为可编辑数组
  const parsed = parseOptions(row.options)
  optionList.value = parsed.length
    ? parsed.map((p) => p.replace(/^[A-H]\.\s*/, ''))
    : []
  dialogVisible.value = true
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function buildOptionsJson() {
  if (!hasOptions.value) return ''
  const arr = optionList.value
    .map((v, i) => ({ key: optKeys[i], value: v }))
    .filter((o) => o.value !== '' && o.value != null)
  return JSON.stringify(arr)
}

async function onSave() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  const payload = { ...form, options: buildOptionsJson() }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateQuestion(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createQuestion(payload)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除题目「${row.title?.slice(0, 20)}…」吗？`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteQuestion(row.id)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.opt-editor {
  width: 100%;
}
.opt-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.opt-key {
  width: 20px;
  text-align: center;
  font-weight: 600;
  color: #606266;
}
.tip {
  font-size: 12px;
  display: block;
  margin-top: 4px;
}
</style>
