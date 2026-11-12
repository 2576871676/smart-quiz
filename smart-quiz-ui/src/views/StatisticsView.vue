<template>
  <div class="page-container" v-loading="loading">
    <!-- 概览卡片 -->
    <el-row :gutter="16" class="mb-12">
      <el-col :xs="12" :sm="8" :md="4" v-for="c in cards" :key="c.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: c.color }">{{ c.value }}<span class="unit">{{ c.unit }}</span></div>
          <div class="stat-label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 趋势 -->
      <el-col :md="16" :xs="24">
        <el-card shadow="never" class="mb-12">
          <template #header>
            <div class="card-head">
              <span class="card-title">答题趋势</span>
              <el-radio-group v-model="days" size="small" @change="loadTrend">
                <el-radio-button :value="7">近 7 天</el-radio-button>
                <el-radio-button :value="14">近 14 天</el-radio-button>
                <el-radio-button :value="30">近 30 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendRef" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 科目正确率 -->
      <el-col :md="8" :xs="24">
        <el-card shadow="never" class="mb-12">
          <template #header><span class="card-title">各科目正确率</span></template>
          <div ref="subjectRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 题型正确率 -->
      <el-col :md="8" :xs="24">
        <el-card shadow="never" class="mb-12">
          <template #header><span class="card-title">题型正确率</span></template>
          <div ref="typeRef" class="chart"></div>
        </el-card>
      </el-col>
      <!-- 难度正确率 -->
      <el-col :md="8" :xs="24">
        <el-card shadow="never" class="mb-12">
          <template #header><span class="card-title">难度正确率</span></template>
          <div ref="diffRef" class="chart"></div>
        </el-card>
      </el-col>
      <!-- 薄弱知识点 -->
      <el-col :md="8" :xs="24">
        <el-card shadow="never" class="mb-12">
          <template #header>
            <div class="card-head">
              <span class="card-title">薄弱知识点 TOP</span>
              <el-select v-model="limit" size="small" style="width: 80px" @change="loadWeak">
                <el-option :value="5" label="5" />
                <el-option :value="10" label="10" />
              </el-select>
            </div>
          </template>
          <div ref="weakRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AI 建议 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span class="card-title"><el-icon><ChatDotRound /></el-icon> AI 学习建议</span>
          <el-button size="small" type="primary" :loading="adviceLoading" :icon="Refresh" @click="loadAdvice">
            重新生成
          </el-button>
        </div>
      </template>
      <pre v-loading="adviceLoading" class="advice-text">{{ advice || '点击右上角生成学习建议' }}</pre>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onActivated, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { getOverview, getAccuracy, getTrend, getWeakPoints, getAiAdvice } from '@/api/statistics'

const loading = ref(false)
const adviceLoading = ref(false)
const overview = ref({})
const advice = ref('')
const days = ref(7)
const limit = ref(5)

const trendRef = ref()
const subjectRef = ref()
const typeRef = ref()
const diffRef = ref()
const weakRef = ref()
const charts = {}

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '累计答题', value: o.totalAnswered ?? 0, unit: '', color: '#409eff' },
    { label: '总正确率', value: o.accuracy ?? 0, unit: '%', color: '#67c23a' },
    { label: '答对', value: o.correctCount ?? 0, unit: '', color: '#67c23a' },
    { label: '答错', value: o.wrongCount ?? 0, unit: '', color: '#f56c6c' },
    { label: '错题本', value: o.wrongBookCount ?? 0, unit: '', color: '#e6a23c' },
    { label: '已掌握', value: o.masteredCount ?? 0, unit: '', color: '#909399' }
  ]
})

function initChart(key, el) {
  if (!el) return null
  if (!charts[key]) charts[key] = echarts.init(el)
  return charts[key]
}

async function loadOverview() {
  overview.value = await getOverview()
}

async function loadTrend() {
  const points = await getTrend(days.value)
  await nextTick()
  const chart = initChart('trend', trendRef.value)
  chart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['答题数', '正确数', '正确率'] },
    grid: { left: 40, right: 45, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: points.map((p) => p.date?.slice(5)), boundaryGap: false },
    yAxis: [
      { type: 'value', name: '题数', minInterval: 1 },
      { type: 'value', name: '正确率%', max: 100, min: 0 }
    ],
    series: [
      { name: '答题数', type: 'line', smooth: true, areaStyle: { opacity: 0.12 }, data: points.map((p) => p.total) },
      { name: '正确数', type: 'line', smooth: true, itemStyle: { color: '#67c23a' }, data: points.map((p) => p.correct) },
      {
        name: '正确率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        itemStyle: { color: '#e6a23c' },
        data: points.map((p) => p.accuracy)
      }
    ]
  })
}

function barOption(data, color) {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: data.map((d) => d.name), axisLabel: { interval: 0, rotate: data.length > 4 ? 20 : 0 } },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        type: 'bar',
        data: data.map((d) => d.accuracy),
        barMaxWidth: 40,
        itemStyle: { color, borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top', formatter: '{c}%' }
      }
    ]
  }
}

async function loadAccuracy() {
  const acc = await getAccuracy()
  await nextTick()
  initChart('subject', subjectRef.value)?.setOption(barOption(acc.bySubject || [], '#409eff'))
  initChart('type', typeRef.value)?.setOption(barOption(acc.byType || [], '#67c23a'))
  initChart('diff', diffRef.value)?.setOption(barOption(acc.byDifficulty || [], '#e6a23c'))
}

async function loadWeak() {
  const weak = await getWeakPoints(limit.value)
  await nextTick()
  const chart = initChart('weak', weakRef.value)
  const data = (weak || []).slice().reverse()
  chart?.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 90, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: data.map((d) => d.knowledgePoint) },
    series: [
      {
        type: 'bar',
        data: data.map((d) => d.wrongCount),
        barMaxWidth: 22,
        itemStyle: { color: '#f56c6c', borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right' }
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

function resizeAll() {
  Object.values(charts).forEach((c) => c?.resize())
}

async function loadAll() {
  loading.value = true
  try {
    await loadOverview()
    await Promise.all([loadTrend(), loadAccuracy(), loadWeak()])
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadAll()
  loadAdvice()
  window.addEventListener('resize', resizeAll)
})

onActivated(() => {
  loadAll()
  nextTick(resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  Object.values(charts).forEach((c) => c?.dispose())
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
}
.unit {
  font-size: 13px;
  font-weight: 400;
  margin-left: 2px;
}
.stat-label {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart {
  height: 300px;
}
.advice-text {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  min-height: 60px;
}
</style>
