<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-card shadow="never" class="mb-12">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索试卷标题"
          clearable
          style="width: 220px"
          :prefix-icon="Search"
          @keyup.enter="reload"
        />
        <el-select v-model="query.subject" placeholder="科目" clearable style="width: 140px">
          <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
        </el-select>
        <el-checkbox v-model="query.onlyMine" border>只看我的</el-checkbox>
        <div class="spacer"></div>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate">新建试卷</el-button>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="title" label="试卷标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="subject" label="科目" width="110" />
        <el-table-column prop="questionCount" label="题数" width="70" align="center" />
        <el-table-column prop="totalScore" label="总分" width="70" align="center" />
        <el-table-column label="时长" width="90" align="center">
          <template #default="{ row }">{{ row.duration }} 分钟</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">查看</el-button>
            <el-button link type="warning" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 1"
              link
              type="success"
              :icon="Upload"
              @click="onPublish(row)"
            >发布</el-button>
            <el-button
              v-else
              link
              type="success"
              :icon="VideoPlay"
              @click="goExam(row)"
            >去考试</el-button>
            <el-button link type="danger" :icon="Delete" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无试卷，点击「新建试卷」开始组卷" />
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

    <!-- 新建/编辑试卷 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑试卷' : '新建试卷'"
      width="820px"
      destroy-on-close
      top="6vh"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="如：数据结构期末模拟卷" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科目">
              <el-select v-model="form.subject" filterable allow-create clearable placeholder="选择或输入" style="width: 100%">
                <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="时长">
              <el-input-number v-model="form.duration" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">草稿</el-radio>
                <el-radio :value="1">直接发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="选填，试卷说明" />
        </el-form-item>

        <el-form-item label="题目">
          <div class="items-editor">
            <div class="items-head">
              <span class="text-muted">
                已选 {{ items.length }} 题，总分 {{ itemsTotal }} 分
              </span>
              <el-button type="primary" size="small" :icon="Plus" @click="openPicker">
                从题库添加
              </el-button>
            </div>
            <el-table v-if="items.length" :data="items" border size="small" style="width: 100%">
              <el-table-column type="index" label="序" width="50" align="center" />
              <el-table-column prop="title" label="题干" min-width="220" show-overflow-tooltip />
              <el-table-column label="题型" width="80" align="center">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="分值" width="120" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="1" :max="100" size="small" controls-position="right" style="width: 100px" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" align="center">
                <template #default="{ $index }">
                  <el-button link :icon="Top" :disabled="$index === 0" @click="moveItem($index, -1)" />
                  <el-button link :icon="Bottom" :disabled="$index === items.length - 1" @click="moveItem($index, 1)" />
                  <el-button link type="danger" :icon="Delete" @click="items.splice($index, 1)" />
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else :image-size="60" description="尚未添加题目" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题目选择器 -->
    <el-dialog v-model="pickerVisible" title="从题库选择题目" width="820px" destroy-on-close top="6vh">
      <div class="toolbar mb-12">
        <el-input v-model="pickerQuery.keyword" placeholder="搜索题干" clearable style="width: 200px" :prefix-icon="Search" @keyup.enter="loadPicker" />
        <el-select v-model="pickerQuery.subject" placeholder="科目" clearable style="width: 130px">
          <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="pickerQuery.type" placeholder="题型" clearable style="width: 110px">
          <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Search" @click="loadPicker">查询</el-button>
      </div>
      <el-table
        ref="pickerTableRef"
        v-loading="pickerLoading"
        :data="pickerList"
        border
        height="360"
        row-key="id"
        @selection-change="onPickerSelect"
      >
        <el-table-column type="selection" width="46" reserve-selection />
        <el-table-column prop="subject" label="科目" width="100" />
        <el-table-column label="题型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="题干" min-width="240" show-overflow-tooltip />
        <el-table-column prop="score" label="默认分" width="70" align="center" />
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="pickerQuery.pageNum"
          :page-size="pickerQuery.pageSize"
          :total="pickerTotal"
          layout="total, prev, pager, next"
          background
          small
          @current-change="loadPicker"
        />
      </div>
      <template #footer>
        <span class="text-muted" style="float: left">已勾选 {{ pickerSelected.length }} 题</span>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker">加入试卷</el-button>
      </template>
    </el-dialog>

    <!-- 试卷详情 -->
    <el-dialog v-model="detailVisible" title="试卷详情" width="760px" top="6vh">
      <el-descriptions v-if="detail" :column="2" border size="small" class="mb-12">
        <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="科目">{{ detail.subject || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '已发布' : '草稿' }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ detail.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="时长">{{ detail.duration }} 分钟</el-descriptions-item>
        <el-descriptions-item label="说明" :span="2">{{ detail.description || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-collapse v-if="detail">
        <el-collapse-item v-for="(it, i) in detail.items || []" :key="it.questionId" :name="it.questionId">
          <template #title>
            <span class="q-no">{{ i + 1 }}</span>
            <el-tag size="small" effect="plain" style="margin: 0 8px">{{ typeLabel(it.type) }}</el-tag>
            <span class="text-muted">({{ it.score }}分)</span>
            <span style="margin-left: 8px">{{ it.title }}</span>
          </template>
          <div v-if="parseOptions(it.options).length" class="d-options">
            <div v-for="(o, oi) in parseOptions(it.options)" :key="oi">{{ o }}</div>
          </div>
          <p><strong>参考答案：</strong>{{ it.answer }}</p>
          <p v-if="it.analysis"><strong>解析：</strong>{{ it.analysis }}</p>
        </el-collapse-item>
      </el-collapse>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Plus, Edit, Delete, View, Upload, VideoPlay, Top, Bottom
} from '@element-plus/icons-vue'
import { pageQuestions } from '@/api/question'
import {
  pagePapers, createPaper, updatePaper, deletePaper,
  getPaperDetail, publishPaper
} from '@/api/exam'
import { SUBJECTS, QUESTION_TYPES, typeLabel, parseOptions } from '@/utils/dict'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, subject: '', status: null, keyword: '', onlyMine: true })

const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref()
const detail = ref(null)
const items = ref([]) // {questionId,title,type,score}

const form = reactive({ title: '', subject: '', description: '', duration: 60, status: 0 })
const rules = { title: [{ required: true, message: '请输入试卷标题', trigger: 'blur' }] }
const itemsTotal = computed(() => items.value.reduce((s, it) => s + (Number(it.score) || 0), 0))

// 题目选择器
const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerTableRef = ref()
const pickerList = ref([])
const pickerTotal = ref(0)
const pickerSelected = ref([])
const pickerQuery = reactive({ pageNum: 1, pageSize: 8, subject: '', type: null, keyword: '' })

function reload() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, subject: '', status: null, keyword: '', onlyMine: true })
  reload()
}

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const page = await pagePapers(params)
    list.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, { title: '', subject: '', description: '', duration: 60, status: 0 })
  items.value = []
  dialogVisible.value = true
}

async function openEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  const d = await getPaperDetail(row.id)
  detail.value = d
  Object.assign(form, {
    title: d.title,
    subject: d.subject || '',
    description: d.description || '',
    duration: d.duration || 60,
    status: d.status ?? 0
  })
  items.value = (d.items || []).map((it) => ({
    questionId: it.questionId,
    title: it.title,
    type: it.type,
    score: it.score
  }))
  dialogVisible.value = true
}

async function openDetail(row) {
  detail.value = await getPaperDetail(row.id)
  detailVisible.value = true
}

function moveItem(index, dir) {
  const target = index + dir
  if (target < 0 || target >= items.value.length) return
  const arr = items.value
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
}

// ---- 题目选择器 ----
function openPicker() {
  pickerSelected.value = []
  pickerQuery.pageNum = 1
  pickerVisible.value = true
  loadPicker()
}

async function loadPicker() {
  pickerLoading.value = true
  try {
    const params = { ...pickerQuery }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const page = await pageQuestions(params)
    pickerList.value = page.records || []
    pickerTotal.value = page.total || 0
  } finally {
    pickerLoading.value = false
  }
}

function onPickerSelect(rows) {
  pickerSelected.value = rows
}

function confirmPicker() {
  const exist = new Set(items.value.map((it) => it.questionId))
  pickerSelected.value.forEach((q) => {
    if (!exist.has(q.id)) {
      items.value.push({ questionId: q.id, title: q.title, type: q.type, score: q.score || 5 })
      exist.add(q.id)
    }
  })
  pickerVisible.value = false
  pickerTableRef.value?.clearSelection()
}

async function onSave() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  if (!items.value.length) {
    ElMessage.warning('试卷至少包含一道题')
    return
  }
  const payload = {
    title: form.title,
    subject: form.subject || null,
    description: form.description || null,
    duration: form.duration,
    status: form.status,
    items: items.value.map((it) => ({ questionId: it.questionId, score: it.score }))
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updatePaper(editingId.value, payload)
      ElMessage.success('试卷已更新')
    } else {
      await createPaper(payload)
      ElMessage.success('试卷已创建')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function onPublish(row) {
  try {
    await ElMessageBox.confirm(`确定发布试卷「${row.title}」吗？发布后可用于考试。`, '发布确认', { type: 'warning' })
  } catch {
    return
  }
  await publishPaper(row.id)
  ElMessage.success('已发布')
  loadList()
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除试卷「${row.title}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  await deletePaper(row.id)
  ElMessage.success('已删除')
  loadList()
}

function goExam(row) {
  router.push({ path: '/exam-taking', query: { paperId: row.id } })
}

onMounted(loadList)
</script>

<style scoped>
.items-editor {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.q-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
}
.d-options {
  color: #606266;
  line-height: 1.8;
  margin-bottom: 8px;
}
</style>
