<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="rec-header">
          <span class="card-title">答题记录</span>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="110" />
        <el-table-column label="题型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="题目" min-width="240" show-overflow-tooltip />
        <el-table-column prop="userAnswer" label="我的答案" width="140" show-overflow-tooltip />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isCorrect === 1 ? 'success' : row.isCorrect === 0 ? 'danger' : 'info'">
              {{ row.isCorrect === 1 ? '正确' : row.isCorrect === 0 ? '错误' : '未判分' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="70" align="center" />
        <el-table-column prop="duration" label="用时(s)" width="90" align="center" />
        <el-table-column label="AI 评语" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.aiComment || '—' }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无答题记录，去「在线答题」练一组吧" /></template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { pageRecords } from '@/api/answer'
import { typeLabel } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function reload() {
  query.pageNum = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const page = await pageRecords({ ...query })
    list.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.rec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
