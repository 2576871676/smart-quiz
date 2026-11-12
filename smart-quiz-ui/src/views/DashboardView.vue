<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :span="24">
        <div class="welcome mb-12">
          <h2>你好，{{ auth.nickname }} 👋</h2>
          <p class="text-muted">这是你的学习概览，继续加油！</p>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="12" :sm="8" :md="4" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">
            {{ card.value }}<span class="stat-unit">{{ card.unit }}</span>
          </div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-12">
      <el-col :md="14" :xs="24">
        <el-card shadow="never">
          <template #header><span class="card-title">近 7 天答题趋势</span></template>
          <div ref="trendRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :md="10" :xs="24">
        <el-card shadow="never">
          <template #header><span class="card-title">快捷入口</span></template>
          <div class="quick-links">
            <el-button type="primary" :icon="MagicStick" @click="$router.push('/ai-generate')">
              AI 出题
            </el-button>
            <el-button type="success" :icon="EditPen" @click="$router.push('/practice')">
              在线答题
            </el-button>
            <el-button type="warning" :icon="WarnTriangleFilled" @click="$router.push('/wrong-book')">
              复习错题
            </el-button>
            <el-button :icon="TrendCharts" @click="$router.push('/statistics')">
              学习统计
            </el-button>
          </div>
          <el-divider />
          <div v-if="advice" class="advice">
            <div class="card-title mb-12">
              <el-icon><ChatDotRound /></el-icon> AI 学习建议
            </div>
            <pre class="advice-text">{{ advice }}</pre>
          </div>
          <el-button v-else text type="primary" :loading="adviceLoading" @click="loadAdvice">
            生成 AI 学习建议
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onActivated, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { MagicStick, EditPen, WarnTriangleFilled, TrendCharts } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { getOverview, getTrend, getAiAdvice } from '@/api/statistics'

const auth = useAuthStore()
const loading = ref(false)
const overview = ref({})
const trendRef = ref()
let chart = null
const advice = ref('')
const adviceLoading = ref(false)

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '累计答题', value: o.totalAnswered ?? 0, unit: ' 题', color: '#409eff' },
    { label: '总正确率', value: o.accuracy ?? 0, unit: ' %', color: '#67c23a' },
    { label: '答对', value: o.correctCount ?? 0, unit: '', color: '#67c23a' },
    { label: '答错', value: o.wrongCount ?? 0, unit: '', color: '#f56c6c' },
    { label: '错题本', value: o.wrongBookCount ?? 0, unit: ' 题', color: '#e6a23c' },
    { label: '近 7 天', value: o.recentAnswered ?? 0, unit: ' 题', color: '#909399' }
  ]
})

async function loadOverview() {
  loading.value = true
  try {
    overview.value = await getOverview()
  } finally {
    loading.value = false
  }
}

async function loadTrend() {
  const points = await getTrend(7)
  await nextTick()
  if (!trendRef.value) return
  if (!chart) chart = echarts.init(trendRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    legend: { data: ['答题数', '正确数'] },
    xAxis: {
      type: 'category',
      data: points.map((p) => p.date?.slice(5)),
      boundaryGap: false
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '答题数',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        data: points.map((p) => p.total)
      },
      {
        name: '正确数',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#67c23a' },
        data: points.map((p) => p.correct)
      }
    ]
  })
}

async function loadAdvice() {
  adviceLoading.value = true
  try {
    advice.value = await getAiAdvice()
  } finally {
    adviceLoading.value = false
  }
}

function resize() {
  chart?.resize()
}

onMounted(async () => {
  await loadOverview()
  await loadTrend()
  window.addEventListener('resize', resize)
})

onActivated(() => {
  loadOverview()
  loadTrend()
})
</script>

<style scoped>
.welcome h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.welcome p {
  margin: 0;
}
.stat-card {
  text-align: center;
  margin-bottom: 16px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-unit {
  font-size: 13px;
  font-weight: 400;
  margin-left: 2px;
}
.stat-label {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}
.chart-box {
  height: 300px;
}
.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.advice-text {
  white-space: pre-wrap;
  word-break: break-word;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  margin: 0;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}
</style>
