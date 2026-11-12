<template>
  <div class="page-container">
    <!-- 统计概览 -->
    <el-row :gutter="16" class="mb-12" v-loading="statLoading">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #f56c6c">{{ stat.totalCount ?? 0 }}</div>
          <div class="stat-label">错题总数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #67c23a">{{ stat.masteredCount ?? 0 }}</div>
          <div class="stat-label">已掌握</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #409eff">{{ stat.masteryRate ?? 0 }}%</div>
          <div class="stat-label">掌握率</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #e6a23c">{{ stat.weakPoints?.length ?? 0 }}</div>
          <div class="stat-label">薄弱知识点</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 + 列表 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.subject" placeholder="科目" clearable style="width: 140px">
          <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="query.knowledgePoint" placeholder="知识点" clearable style="width: 160px" />
        <el-select v-model="query.mastered" placeholder="掌握状态" clearable style="width: 130px">
          <el-option label="未掌握" :value="false" />
          <el-option label="已掌握" :value="true" />
        </el-select>
        <el-select v-model="query.sortBy" placeholder="排序" style="width: 150px">
          <el-option label="最近错误时间" value="lastWrongTime" />
          <el-option label="错误次数" value="wrongCount" />
        </el-select>
        <div class="spacer"></div>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="subject" label="科目" width="110" />
        <el-table-column prop="knowledgePoint" label="知识点" width="130" show-overflow-tooltip />
        <el-table-column label="题型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="题目" min-width="220" show-overflow-tooltip />
        <el-table-column prop="wrongCount" label="错误次数" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="danger" effect="plain">{{ row.wrongCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.mastered === 1 ? 'success' : 'warning'">
              {{ row.mastered === 1 ? '已掌握' : '未掌握' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近错误" width="170">
          <template #default="{ row }">{{ formatTime(row.lastWrongTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.mastered !== 1"
              link
              type="success"
              :icon="Check"
              @click="onMaster(row)"
            >
              标记掌握
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="onRemove(row)">移除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="错题本是空的，保持住！" /></template>
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

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="错题详情" width="600px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="科目">{{ detail.subject }}</el-descriptions-item>
        <el-descriptions-item label="知识点">{{ detail.knowledgePoint }}</el-descriptions-item>
        <el-descriptions-item label="题型">{{ typeLabel(detail.type) }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ difficultyLabel(detail.difficulty) }}</el-descriptions-item>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, View, Check, Delete } from '@element-plus/icons-vue'
import {
  pageWrongQuestions,
  getWrongStatistics,
  markMastered,
  removeWrongQuestion
} from '@/api/wrongQuestion'
import {
  SUBJECTS,
  typeLabel,
  difficultyLabel,
  parseOptions
} from '@/utils/dict'

const loading = ref(false)
const statLoading = ref(false)
const list = ref([])
const total = ref(0)
const stat = ref({})
const detailVisible = ref(false)
const detail = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  subject: '',
  knowledgePoint: '',
  mastered: null,
  sortBy: 'lastWrongTime'
})

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    subject: '',
    knowledgePoint: '',
    mastered: null,
    sortBy: 'lastWrongTime'
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
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const page = await pageWrongQuestions(params)
    list.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStat() {
  statLoading.value = true
  try {
    stat.value = await getWrongStatistics()
  } finally {
    statLoading.value = false
  }
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function onMaster(row) {
  await markMastered(row.questionId)
  ElMessage.success('已标记为掌握')
  loadList()
  loadStat()
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm('确定从错题本移除该题吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await removeWrongQuestion(row.questionId)
  ElMessage.success('已移除')
  loadList()
  loadStat()
}

onMounted(() => {
  loadList()
  loadStat()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
}
.stat-label {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}
</style>
