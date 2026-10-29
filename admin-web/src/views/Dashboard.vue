<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import http from '../api/http'

const currentPeriod = ref('today')
const periods = [
  { value: 'today', label: '当日' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'all', label: '总榜' }
]

const overviewLoading = ref(false)
const satisfactionLoading = ref(false)
const overviewData = ref({
  visitorCount: 0,
  chatCount: 0,
  topQuestions: [],
  topScenics: []
})
const satisfactionTrend = ref([])

let satisfactionChart = null
let scenicChart = null

const formatNumber = (num) => {
  if (!num) return '0'
  return Number(num).toLocaleString()
}

const fetchSatisfactionTrend = async () => {
  satisfactionLoading.value = true
  try {
    const res = await http.get('/admin/dashboard/satisfaction-trend')
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true || Boolean(result?.data)
    if (!ok) throw new Error(result?.msg || result?.message || '获取满意度趋势失败')
    satisfactionTrend.value = result?.data || []
  } catch (e) {
    ElMessage.error(e?.message || '获取满意度趋势失败')
  } finally {
    satisfactionLoading.value = false
  }
}

const initCharts = () => {
  setTimeout(() => {
    initSatisfactionChart()
    initScenicChart()
  }, 100)
}

const fetchOverview = async () => {
  overviewLoading.value = true
  try {
    const res = await http.get('/admin/dashboard/overview', {
      params: { period: currentPeriod.value }
    })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true || Boolean(result?.data)
    if (!ok) throw new Error(result?.msg || result?.message || '获取数据大屏失败')
    overviewData.value = result?.data || { visitorCount: 0, chatCount: 0, topQuestions: [], topScenics: [] }
    await nextTick()
    initScenicChart()
  } catch (e) {
    ElMessage.error(e?.message || '获取数据大屏失败')
  } finally {
    overviewLoading.value = false
  }
}

const initSatisfactionChart = () => {
  const dom = document.getElementById('satisfaction-chart')
  if (!dom) {
    console.warn('satisfaction-chart DOM not found')
    return
  }
  const rect = dom.getBoundingClientRect()
  if (rect.height === 0) {
    console.warn('satisfaction-chart has no height')
    return
  }
  if (satisfactionChart) {
    satisfactionChart.dispose()
  }
  satisfactionChart = echarts.init(dom)
  
  const dates = satisfactionTrend.value.map(item => {
    const date = item.statDate
    if (typeof date === 'string') {
      return date.slice(5)
    }
    return ''
  })
  const scores = satisfactionTrend.value.map(item => item.avgScore || 0)
  
  const option = {
    backgroundColor: 'transparent',
    grid: {
      top: 30,
      right: 30,
      bottom: 40,
      left: 50
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: '#374151'
      },
      formatter: (params) => {
        const data = params[0]
        const date = satisfactionTrend.value[data.dataIndex]?.statDate || ''
        const chatCount = satisfactionTrend.value[data.dataIndex]?.chatCount || 0
        return `<div style="padding: 8px;">
          <div style="font-weight: 600; margin-bottom: 8px;">${date}</div>
          <div>满意度: <span style="color: #1b5e20; font-weight: 600;">${data.value.toFixed(2)}</span> 分</div>
          <div>对话数: <span style="color: #2563eb; font-weight: 600;">${chatCount}</span> 次</div>
        </div>`
      }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: {
        lineStyle: {
          color: '#e5e7eb'
        }
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 3,
      interval: 1,
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        type: 'line',
        data: scores,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#1b5e20',
          width: 3
        },
        itemStyle: {
          color: '#1b5e20',
          borderWidth: 2,
          borderColor: '#fff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(27, 94, 32, 0.25)' },
            { offset: 1, color: 'rgba(27, 94, 32, 0.02)' }
          ])
        }
      }
    ]
  }
  
  satisfactionChart.setOption(option)
  
  window.addEventListener('resize', () => {
    satisfactionChart?.resize()
  })
}

const initScenicChart = () => {
  const dom = document.getElementById('scenic-chart')
  if (!dom) return
  if (scenicChart) {
    scenicChart.dispose()
  }
  scenicChart = echarts.init(dom)
  
  const data = overviewData.value.topScenics || []
  const names = data.map(item => item.scenicName || item.name || '未知景点')
  const counts = data.map(item => item.visitCount || item.count || 0)
  
  const option = {
    backgroundColor: 'transparent',
    grid: {
      top: 20,
      right: 20,
      bottom: 40,
      left: 80
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: '#374151'
      },
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        const data = params[0]
        return `<div style="padding: 8px;">
          <div style="font-weight: 600; margin-bottom: 4px;">${data.name}</div>
          <div>访问次数: <span style="color: #1b5e20; font-weight: 600;">${data.value}</span> 次</div>
        </div>`
      }
    },
    xAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6',
          type: 'dashed'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: names.reverse(),
      axisLine: {
        lineStyle: {
          color: '#e5e7eb'
        }
      },
      axisLabel: {
        color: '#374151',
        fontSize: 12
      },
      axisTick: {
        show: false
      }
    },
    series: [
      {
        type: 'bar',
        data: counts.reverse(),
        barWidth: '50%',
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#1b5e20' },
            { offset: 1, color: '#2e7d32' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          color: '#6b7280',
          fontSize: 12,
          fontWeight: 600
        }
      }
    ]
  }
  
  scenicChart.setOption(option)
  
  window.addEventListener('resize', () => {
    scenicChart?.resize()
  })
}

const currentPeriodLabel = computed(() => {
  const period = periods.find(p => p.value === currentPeriod.value)
  return period?.label || '当日'
})

watch(currentPeriod, () => {
  fetchOverview()
})

watch(satisfactionTrend, () => {
  if (satisfactionTrend.value.length > 0) {
    initSatisfactionChart()
  }
}, { deep: true })

watch(() => overviewData.value.topScenics, () => {
  initScenicChart()
}, { deep: true })

onMounted(() => {
  fetchSatisfactionTrend()
  fetchOverview()
  initCharts()
  
  window.addEventListener('resize', () => {
    satisfactionChart?.resize()
    scenicChart?.resize()
  })
})
</script>

<template>
  <div class="dashboard-page">
    <div class="page-head">
      <div class="page-title">数据大屏概览</div>
      <div class="page-sub">景区服务数据统计与分析</div>
    </div>

    <div class="chart-section">
      <el-card class="chart-card" shadow="never">
        <div class="card-header">
          <div class="card-title">游客满意度趋势</div>
          <div class="card-sub">近30天满意度变化</div>
        </div>
        <div class="chart-container">
          <div id="satisfaction-chart" class="chart"></div>
        </div>
      </el-card>
    </div>

    <div class="period-tabs">
      <el-button
        v-for="period in periods"
        :key="period.value"
        :class="['period-btn', { active: currentPeriod === period.value }]"
        @click="currentPeriod = period.value"
      >
        {{ period.label }}
      </el-button>
    </div>

    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon visitor">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">服务人次</div>
          <div class="stat-value">{{ formatNumber(overviewData.visitorCount) }}</div>
          <div class="stat-period">{{ currentPeriodLabel }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon chat">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">对话次数</div>
          <div class="stat-value">{{ formatNumber(overviewData.chatCount) }}</div>
          <div class="stat-period">{{ currentPeriodLabel }}</div>
        </div>
      </div>
    </div>

    <div class="bottom-section">
      <el-card class="question-card" shadow="never">
        <div class="card-header">
          <div class="card-title">热门问答 TOP10</div>
          <div class="card-sub">{{ currentPeriodLabel }}热门问题排行</div>
        </div>
        <div class="question-list">
          <div
            v-for="(item, index) in overviewData.topQuestions"
            :key="index"
            class="question-item"
          >
            <div class="question-rank" :class="{ top: index < 3 }">{{ index + 1 }}</div>
            <div class="question-content">
              <div class="question-text">{{ item.question || item.content || '暂无数据' }}</div>
              <div class="question-count">被问 {{ item.totalCount || 0 }} 次</div>
            </div>
          </div>
          <div v-if="!overviewData.topQuestions.length" class="empty-tip">暂无数据</div>
        </div>
      </el-card>

      <el-card class="scenic-card" shadow="never">
        <div class="card-header">
          <div class="card-title">热门景点 TOP5</div>
          <div class="card-sub">{{ currentPeriodLabel }}热门景点排行</div>
        </div>
        <div class="chart-container">
          <div id="scenic-chart" class="chart"></div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding-bottom: 20px;
}

.page-head {
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #123019;
  letter-spacing: 0.4px;
}

.page-sub {
  margin-top: 6px;
  color: rgba(18, 48, 25, 0.6);
  font-size: 13px;
}

.period-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.period-btn {
  padding: 8px 20px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  background: #ffffff;
  border: 1px solid rgba(18, 48, 25, 0.1);
  color: rgba(18, 48, 25, 0.7);
  transition: all 0.3s;
}

.period-btn:hover {
  border-color: rgba(27, 94, 32, 0.3);
  color: #1b5e20;
}

.period-btn.active {
  background: linear-gradient(90deg, #1b5e20, #2e7d32);
  border-color: #1b5e20;
  color: #ffffff;
}

.chart-section {
  margin-bottom: 16px;
}

.chart-card {
  border: 1px solid rgba(18, 48, 25, 0.08);
  border-radius: 14px;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(18, 48, 25, 0.06);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #123019;
}

.card-sub {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(18, 48, 25, 0.5);
}

.chart-container {
  padding: 16px;
}

.chart {
  width: 100%;
  height: 280px;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #fafbf9 100%);
  border: 1px solid rgba(18, 48, 25, 0.08);
  border-radius: 14px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.visitor {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.stat-icon.chat {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: rgba(18, 48, 25, 0.6);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #123019;
  font-variant-numeric: tabular-nums;
  letter-spacing: -1px;
}

.stat-period {
  font-size: 12px;
  color: rgba(18, 48, 25, 0.45);
  margin-top: 4px;
}

.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.question-card,
.scenic-card {
  border: 1px solid rgba(18, 48, 25, 0.08);
  border-radius: 14px;
}

.question-list {
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.question-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: rgba(18, 48, 25, 0.02);
  border-radius: 10px;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.question-item:hover {
  background: rgba(18, 48, 25, 0.05);
}

.question-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.question-rank.top {
  background: linear-gradient(135deg, #1b5e20, #2e7d32);
  color: #ffffff;
}

.question-content {
  flex: 1;
  min-width: 0;
}

.question-text {
  font-size: 13px;
  color: #123019;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.question-count {
  font-size: 12px;
  color: rgba(18, 48, 25, 0.5);
  margin-top: 6px;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: rgba(18, 48, 25, 0.4);
  font-size: 13px;
}

@media (max-width: 1024px) {
  .bottom-section {
    grid-template-columns: 1fr;
  }
  
  .stats-section {
    grid-template-columns: 1fr;
  }
}
</style>
