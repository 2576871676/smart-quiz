<template>
  <div class="page-container">
    <!-- 可参加的考试（已发布试卷） -->
    <el-card shadow="never" class="mb-12">
      <template #header>
        <div class="card-header">
          <span class="card-title">考试中心 · 已发布试卷</span>
          <div>
            <el-input
              v-model="query.keyword"
              placeholder="搜索试卷标题"
              clearable
              size="small"
              style="width: 200px"
              :prefix-icon="Search"
              @keyup.enter="reload"
            />
            <el-button size="small" :icon="Refresh" style="margin-left: 8px" @click="reload">刷新</el-button>
          </div>
        </div>
      </template>
      <el-row v-loading="loading" :gutter="12">
        <el-col v-for="p in list" :key="p.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="paper-card" shadow="hover">
            <div class="paper-title">{{ p.title }}</div>
            <div class="paper-meta">
              <el-tag size="small" effect="plain">{{ p.subject || '综合' }}</el-tag>
              <span class="text-muted">{{ p.questionCount }} 题 · {{ p.totalScore }} 分</span>
            </div>
            <div class="paper-meta text-muted">
              <el-icon><Clock /></el-icon> 限时 {{ p.duration }} 分钟
            </div>
            <p v-if="p.description" class="paper-desc">{{ p.description }}</p>
            <el-button type="primary" :icon="VideoPlay" style="width: 100%" @click="goExam(p)">
              开始考试
            </el-button>
          </el-card>
        </el-col>
        <el-col v-if="!loading && !list.length" :span="24">
          <el-empty description="暂无已发布试卷，请先到「试卷管理」创建并发布" />
        </el-col>
      </el-row>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          small
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 我的考试记录 -->
    <el-card shadow="never">
      <template #header><span class="card-title">我的考试记录</span></template>
      <el-table v-loading="recLoading" :data="records" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="paperTitle" label="试卷" min-width="180" show-overflow-tooltip />
        <el-table-column prop="subject" label="科目" width="110" />
        <el-table-column label="得分" width="90" align="center">
          <template #default="{ row }">
            <span class="score-text">{{ row.status === 1 ? row.totalScore : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="对/错" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 1" class="text-muted">{{ row.correctCount }} / {{ row.wrongCount }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="用时" width="90" align="center">
          <template #default="{ row }">{{ row.status === 1 ? formatDuration(row.duration) : '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已交卷' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" width="170">
          <template #default="{ row }">{{ row.submitTime || '—' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有考试记录" />
        </template>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="recQuery.pageNum"
          :page-size="recQuery.pageSize"
          :total="recTotal"
          layout="total, prev, pager, next"
          background
          small
          @current-change="loadRecords"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh, VideoPlay, Clock } from '@element-plus/icons-vue'
import { pagePapers, pageExamRecords } from '@/api/exam'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 8, status: 1, keyword: '' })

const recLoading = ref(false)
const records = ref([])
const recTotal = ref(0)
const recQuery = reactive({ pageNum: 1, pageSize: 10 })

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
    const page = await pagePapers(params)
    list.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  recLoading.value = true
  try {
    const page = await pageExamRecords({ ...recQuery })
    records.value = page.records || []
    recTotal.value = page.total || 0
  } finally {
    recLoading.value = false
  }
}

function formatDuration(sec) {
  if (sec == null) return '—'
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

function goExam(p) {
  router.push({ path: '/exam-taking', query: { paperId: p.id } })
}

function refreshAll() {
  loadList()
  loadRecords()
}

onMounted(refreshAll)
// keep-alive 缓存后返回本页时刷新记录（考试完成回来能看到新记录）
onActivated(() => {
  loadRecords()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.paper-card {
  margin-bottom: 12px;
}
.paper-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 8px;
  line-height: 1.4;
  min-height: 42px;
}
.paper-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 13px;
}
.paper-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin: 4px 0 10px;
  height: 36px;
  overflow: hidden;
}
.score-text {
  font-weight: 700;
  color: #409eff;
}
</style>
