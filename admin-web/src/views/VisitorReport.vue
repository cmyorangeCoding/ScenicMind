<script setup>
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import http from '../api/http'

const currentPeriod = ref('week')
const periods = [
  { value: 'day', label: '今日' },
  { value: 'week', label: '近7天' },
  { value: 'month', label: '近30天' }
]

const loading = ref(false)
const aiLoading = ref(false)
const exportLoading = ref(false)
const reportData = ref({
  emotionStats: null,
  topQuestions: [],
  topScenics: [],
  complaintStats: []
})
const aiSuggestions = ref('')

let emotionChart = null
let scenicChart = null

const currentPeriodLabel = computed(() => {
  const period = periods.find(p => p.value === currentPeriod.value)
  return period?.label || '近7天'
})

const totalComplaints = computed(() => {
  return reportData.value.complaintStats.reduce((sum, item) => sum + (item.count || 0), 0)
})

const totalPending = computed(() => {
  return reportData.value.complaintStats.reduce((sum, item) => sum + (item.pendingCount || 0), 0)
})

const totalProcessing = computed(() => {
  return reportData.value.complaintStats.reduce((sum, item) => sum + (item.processingCount || 0), 0)
})

const emotionTotal = computed(() => {
  const stats = reportData.value.emotionStats || {}
  return (stats.satisfied || 0) + (stats.normal || 0) + (stats.dissatisfied || 0) || 1
})

const satisfiedPercent = computed(() => {
  const stats = reportData.value.emotionStats || {}
  if (stats.satisfiedPercentage !== undefined) {
    return stats.satisfiedPercentage.toFixed(1)
  }
  return ((stats.satisfied || 0) / emotionTotal.value * 100).toFixed(1)
})

const normalPercent = computed(() => {
  const stats = reportData.value.emotionStats || {}
  if (stats.normalPercentage !== undefined) {
    return stats.normalPercentage.toFixed(1)
  }
  return ((stats.normal || 0) / emotionTotal.value * 100).toFixed(1)
})

const dissatisfiedPercent = computed(() => {
  const stats = reportData.value.emotionStats || {}
  if (stats.dissatisfiedPercentage !== undefined) {
    return stats.dissatisfiedPercentage.toFixed(1)
  }
  return ((stats.dissatisfied || 0) / emotionTotal.value * 100).toFixed(1)
})

const fetchReport = async () => {
  loading.value = true
  try {
    const res = await http.get('/admin/dashboard/report/visitor', {
      params: { period: currentPeriod.value }
    })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true || Boolean(result?.data)
    if (!ok) throw new Error(result?.msg || result?.message || '获取游客感受度报告失败')
    reportData.value = result?.data || { emotionStats: null, topQuestions: [], topScenics: [], complaintStats: [] }
    await nextTick()
    initCharts()
  } catch (e) {
    ElMessage.error(e?.message || '获取游客感受度报告失败')
  } finally {
    loading.value = false
  }
}

const fetchAiSuggestions = async () => {
  aiLoading.value = true
  try {
    const res = await http.get('/admin/dashboard/report/ai-suggestions', {
      params: { period: currentPeriod.value }
    })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true || Boolean(result?.data)
    if (!ok) throw new Error(result?.msg || result?.message || '获取AI建议失败')
    aiSuggestions.value = result?.data || ''
  } catch (e) {
    ElMessage.error(e?.message || '获取AI建议失败')
  } finally {
    aiLoading.value = false
  }
}

const exportCSV = () => {
  try {
    const date = new Date()
    const dateStr = date.toLocaleDateString('zh-CN').replace(/\//g, '-')
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    const timestamp = year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds
    
    const rows = []
    
    rows.push(['游客感受度报告', '', '', '', ''])
    rows.push(['统计周期', currentPeriodLabel.value, '', '', ''])
    rows.push(['生成时间', '"' + timestamp + '"', '', '', ''])
    rows.push(['', '', '', '', ''])
    
    rows.push(['一、情感统计', '', '', '', ''])
    rows.push(['类型', '数量', '百分比', '', ''])
    const emotionStats = reportData.value.emotionStats || {}
    const satPercent = String(satisfiedPercent.value)
    const norPercent = String(normalPercent.value)
    const disPercent = String(dissatisfiedPercent.value)
    rows.push(['满意', emotionStats.satisfied || 0, satPercent + '%', '', ''])
    rows.push(['一般', emotionStats.normal || 0, norPercent + '%', '', ''])
    rows.push(['不满', emotionStats.dissatisfied || 0, disPercent + '%', '', ''])
    const total = emotionStats.totalCount || ((emotionStats.satisfied || 0) + (emotionStats.normal || 0) + (emotionStats.dissatisfied || 0))
    rows.push(['总计', total, '', '', ''])
    rows.push(['', '', '', '', ''])
    
    rows.push(['二、热门问题 TOP10', '', '', '', ''])
    rows.push(['排名', '问题内容', '提问次数', '', ''])
    if (reportData.value.topQuestions.length) {
      reportData.value.topQuestions.forEach((item, index) => {
        const question = (item.question || item.content || '').replace(/,/g, '，')
        rows.push([index + 1, question, item.totalCount || 0, '', ''])
      })
    } else {
      rows.push(['', '暂无数据', '', '', ''])
    }
    rows.push(['', '', '', '', ''])
    
    rows.push(['三、热门景点 TOP5', '', '', '', ''])
    rows.push(['排名', '景点名称', '咨询量', '', ''])
    if (reportData.value.topScenics.length) {
      reportData.value.topScenics.forEach((item, index) => {
        const name = (item.scenicName || item.name || '').replace(/,/g, '，')
        rows.push([index + 1, name, item.visitCount || item.count || 0, '', ''])
      })
    } else {
      rows.push(['', '暂无数据', '', '', ''])
    }
    rows.push(['', '', '', '', ''])
    
    rows.push(['四、投诉统计', '', '', '', ''])
    rows.push(['排名', '投诉类型', '投诉数量', '待处理', '处理中'])
    if (reportData.value.complaintStats.length) {
      reportData.value.complaintStats.forEach((item, index) => {
        const type = (item.complaintType || '').replace(/,/g, '，')
        rows.push([index + 1, type, item.count || 0, item.pendingCount || 0, item.processingCount || 0])
      })
    } else {
      rows.push(['', '暂无数据', '', '', ''])
    }
    rows.push(['', '', '', '', ''])
    
    if (aiSuggestions.value) {
      rows.push(['五、AI服务建议', '', '', '', ''])
      const suggestions = aiSuggestions.value.replace(/,/g, '，').replace(/\n/g, ' ')
      rows.push([suggestions, '', '', '', ''])
    }
    
    const csvContent = rows.map(row => {
      return row.map(cell => {
        if (typeof cell === 'string' && cell.includes(',')) {
          return '"' + cell + '"'
        }
        return cell
      }).join(',')
    }).join('\n')
    
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '游客感受度报告_' + currentPeriodLabel.value + '_' + dateStr + '.csv'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('CSV报告导出成功')
  } catch (e) {
    ElMessage.error(e?.message || '导出失败')
  }
}

const exportPDF = () => {
  try {
    const dateStr = new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')
    const timestamp = new Date().toLocaleString('zh-CN')
    const label = currentPeriodLabel.value
    
    const emotionStats = reportData.value.emotionStats || {}
    const satPercent = String(satisfiedPercent.value)
    const norPercent = String(normalPercent.value)
    const disPercent = String(dissatisfiedPercent.value)
    
    let questionsHtml = ''
    if (reportData.value.topQuestions.length) {
      reportData.value.topQuestions.forEach((item, index) => {
        const question = (item.question || item.content || '').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        questionsHtml += `<tr><td>${index + 1}</td><td>${question}</td><td>${item.totalCount || 0}</td></tr>`
      })
    } else {
      questionsHtml = '<tr><td colspan="3" class="empty-text">暂无数据</td></tr>'
    }
    
    let scenicsHtml = ''
    if (reportData.value.topScenics.length) {
      reportData.value.topScenics.forEach((item, index) => {
        const name = (item.scenicName || item.name || '').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        scenicsHtml += `<tr><td>${index + 1}</td><td>${name}</td><td>${item.visitCount || item.count || 0}</td></tr>`
      })
    } else {
      scenicsHtml = '<tr><td colspan="3" class="empty-text">暂无数据</td></tr>'
    }
    
    let complaintsHtml = ''
    if (reportData.value.complaintStats.length) {
      reportData.value.complaintStats.forEach((item, index) => {
        const type = (item.complaintType || '').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        complaintsHtml += `<tr><td>${index + 1}</td><td>${type}</td><td>${item.count || 0}</td><td>${item.pendingCount || 0}</td><td>${item.processingCount || 0}</td></tr>`
      })
    } else {
      complaintsHtml = '<tr><td colspan="5" class="empty-text">暂无数据</td></tr>'
    }
    
    let suggestionsHtml = ''
    if (aiSuggestions.value) {
      const suggestions = aiSuggestions.value.replace(/</g, '&lt;').replace(/>/g, '&gt;')
      suggestionsHtml = `
        <div class="section">
          <div class="section-title">五、AI服务建议</div>
          <div class="suggestions-box">${suggestions}</div>
        </div>
      `
    }
    
    const htmlContent = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>游客感受度报告</title>
  <style>
    body { font-family: "Microsoft YaHei", "SimHei", sans-serif; margin: 0; padding: 40px; background: #fff; }
    .report-header { text-align: center; margin-bottom: 40px; padding-bottom: 20px; border-bottom: 2px solid #1b5e20; }
    .report-title { font-size: 24px; font-weight: bold; color: #123019; }
    .report-meta { margin-top: 10px; color: #666; font-size: 14px; }
    .section { margin-bottom: 30px; }
    .section-title { font-size: 16px; font-weight: bold; color: #123019; margin-bottom: 15px; padding-left: 10px; border-left: 4px solid #1b5e20; }
    .table { width: 100%; border-collapse: collapse; margin-top: 10px; }
    .table th, .table td { border: 1px solid #ddd; padding: 10px; text-align: center; font-size: 13px; }
    .table th { background: #f5f7f5; font-weight: bold; color: #333; }
    .emotion-summary { display: flex; justify-content: space-around; margin-top: 20px; }
    .emotion-card { text-align: center; padding: 20px; background: #f9f9f9; border-radius: 8px; min-width: 120px; }
    .emotion-value { font-size: 28px; font-weight: bold; color: #123019; }
    .emotion-label { font-size: 14px; color: #666; margin-top: 5px; }
    .emotion-percent { font-size: 14px; color: #1b5e20; }
    .suggestions-box { background: #f9f9f9; padding: 20px; border-radius: 8px; margin-top: 10px; line-height: 1.8; font-size: 14px; color: #333; white-space: pre-wrap; }
    .empty-text { text-align: center; color: #999; padding: 20px; }
    @media print {
      body { padding: 20px; }
    }
  </style>
</head>
<body>
  <div class="report-header">
    <div class="report-title">游客感受度报告</div>
    <div class="report-meta">统计周期：${label} | 生成时间：${timestamp}</div>
  </div>
  
  <div class="section">
    <div class="section-title">一、情感统计</div>
    <div class="emotion-summary">
      <div class="emotion-card">
        <div class="emotion-value">${emotionStats.satisfied || 0}</div>
        <div class="emotion-label">满意</div>
        <div class="emotion-percent">${satPercent}%</div>
      </div>
      <div class="emotion-card">
        <div class="emotion-value">${emotionStats.normal || 0}</div>
        <div class="emotion-label">一般</div>
        <div class="emotion-percent">${norPercent}%</div>
      </div>
      <div class="emotion-card">
        <div class="emotion-value">${emotionStats.dissatisfied || 0}</div>
        <div class="emotion-label">不满</div>
        <div class="emotion-percent">${disPercent}%</div>
      </div>
    </div>
  </div>
  
  <div class="section">
    <div class="section-title">二、热门问题 TOP10</div>
    <table class="table">
      <tr><th>排名</th><th>问题内容</th><th>提问次数</th></tr>
      ${questionsHtml}
    </table>
  </div>
  
  <div class="section">
    <div class="section-title">三、热门景点 TOP5</div>
    <table class="table">
      <tr><th>排名</th><th>景点名称</th><th>咨询量</th></tr>
      ${scenicsHtml}
    </table>
  </div>
  
  <div class="section">
    <div class="section-title">四、投诉统计</div>
    <table class="table">
      <tr><th>排名</th><th>投诉类型</th><th>投诉数量</th><th>待处理</th><th>处理中</th></tr>
      ${complaintsHtml}
    </table>
  </div>
  
  ${suggestionsHtml}
</body>
</html>
    `
    
    const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '游客感受度报告_' + label + '_' + dateStr + '.html'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('报告导出成功！打开HTML文件后按Ctrl+P可打印为PDF')
  } catch (e) {
    ElMessage.error(e?.message || '导出失败')
  }
}

const exportReport = async () => {
  exportLoading.value = true
  try {
    await ElMessageBox.confirm(
      '请选择导出格式',
      '导出格式选择',
      {
        confirmButtonText: '导出CSV',
        cancelButtonText: '导出HTML',
        type: 'info',
        customClass: 'export-format-dialog'
      }
    ).then(() => {
      exportCSV()
    }).catch(() => {
      exportPDF()
    })
  } finally {
    exportLoading.value = false
  }
}

const showExportConfirm = () => {
  ElMessageBox.confirm(
    '确定要导出【' + currentPeriodLabel.value + '】的游客感受度报告吗？',
    '导出确认',
    {
      confirmButtonText: '确定导出',
      cancelButtonText: '取消',
      type: 'info',
      confirmButtonClass: 'export-confirm-btn',
      cancelButtonClass: 'export-cancel-btn'
    }
  ).then(() => {
    exportReport()
  }).catch(() => {
    ElMessage.info('已取消导出')
  })
}

const initCharts = () => {
  initEmotionChart()
  initScenicChart()
}

const initEmotionChart = () => {
  const dom = document.getElementById('emotion-chart')
  if (!dom) return
  if (emotionChart) {
    emotionChart.dispose()
  }
  emotionChart = echarts.init(dom)
  
  const emotionData = reportData.value.emotionStats || {}
  const satisfied = emotionData.satisfied || 0
  const normal = emotionData.normal || 0
  const dissatisfied = emotionData.dissatisfied || 0
  const total = satisfied + normal + dissatisfied
  const data = [
    { value: satisfied, name: '满意', itemStyle: { color: '#1b5e20' } },
    { value: normal, name: '一般', itemStyle: { color: '#f59e0b' } },
    { value: dissatisfied, name: '不满', itemStyle: { color: '#ef4444' } }
  ]
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: '#374151'
      },
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center',
      textStyle: {
        color: '#374151',
        fontSize: 12
      }
    },
    series: [
      {
        name: '情感分布',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'inside',
          fontSize: 12,
          fontWeight: 'bold',
          color: '#fff',
          formatter: (params) => {
            const percent = total > 0 ? ((params.value / total) * 100).toFixed(1) : 0
            return `${percent}%`
          }
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
            color: '#123019'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  }
  
  emotionChart.setOption(option)
  
  window.addEventListener('resize', () => {
    emotionChart?.resize()
  })
}

const initScenicChart = () => {
  const dom = document.getElementById('scenic-chart-report')
  if (!dom) return
  if (scenicChart) {
    scenicChart.dispose()
  }
  scenicChart = echarts.init(dom)
  
  const data = reportData.value.topScenics || []
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
          <div>咨询量: <span style="color: #1b5e20; font-weight: 600;">${data.value}</span> 次</div>
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
          borderRadius: [0, 8, 8, 0],
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

const formatNumber = (num) => {
  if (!num) return '0'
  return Number(num).toLocaleString()
}

watch(currentPeriod, () => {
  aiSuggestions.value = ''
  fetchReport()
})

onMounted(() => {
  fetchReport()
})
</script>

<template>
  <div class="report-page">
    <div class="report-header">
      <div class="report-title-section">
        <div class="report-title">游客感受度报告</div>
        <div class="report-subtitle">基于游客对话数据的情感分析与服务建议</div>
        <div class="report-meta">
          <span class="meta-item">统计周期：{{ currentPeriodLabel }}</span>
          <span class="meta-item">生成时间：{{ new Date().toLocaleString('zh-CN') }}</span>
        </div>
      </div>
      <div class="report-actions">
        <div class="period-tabs">
          <el-button
            v-for="period in periods"
            :key="period.value"
            :class="['period-btn', { active: currentPeriod === period.value }]"
            @click="currentPeriod = period.value"
            :loading="loading"
          >
            {{ period.label }}
          </el-button>
        </div>
        <el-button
          type="primary"
          class="export-btn"
          @click="showExportConfirm"
          :loading="exportLoading"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="export-icon">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          导出报告
        </el-button>
      </div>
    </div>

    <div class="report-content" v-loading="loading">
      <div class="section-card emotion-section">
        <div class="section-header">
          <div class="section-title">一、情感统计</div>
          <div class="section-subtitle">游客对话情感分布分析</div>
        </div>
        <div class="emotion-content">
          <div class="emotion-chart-wrapper">
            <div id="emotion-chart" class="emotion-chart"></div>
          </div>
          <div class="emotion-summary">
            <div class="summary-item">
              <div class="summary-label">满意度概况</div>
              <div class="summary-bar">
                <div class="bar-segment satisfied" :style="{ width: satisfiedPercent + '%' }"></div>
                <div class="bar-segment normal" :style="{ width: normalPercent + '%' }"></div>
                <div class="bar-segment dissatisfied" :style="{ width: dissatisfiedPercent + '%' }"></div>
              </div>
            </div>
            <div class="emotion-cards">
              <div class="emotion-card satisfied">
                <div class="emotion-icon"></div>
                <div class="emotion-info">
                  <div class="emotion-label">满意</div>
                  <div class="emotion-value">{{ formatNumber(reportData.emotionStats?.satisfied || 0) }}</div>
                  <div class="emotion-percent">{{ satisfiedPercent }}%</div>
                </div>
              </div>
              <div class="emotion-card normal">
                <div class="emotion-icon"></div>
                <div class="emotion-info">
                  <div class="emotion-label">一般</div>
                  <div class="emotion-value">{{ formatNumber(reportData.emotionStats?.normal || 0) }}</div>
                  <div class="emotion-percent">{{ normalPercent }}%</div>
                </div>
              </div>
              <div class="emotion-card dissatisfied">
                <div class="emotion-icon"></div>
                <div class="emotion-info">
                  <div class="emotion-label">不满</div>
                  <div class="emotion-value">{{ formatNumber(reportData.emotionStats?.dissatisfied || 0) }}</div>
                  <div class="emotion-percent">{{ dissatisfiedPercent }}%</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="section-row">
        <div class="section-card questions-section">
          <div class="section-header">
            <div class="section-title">二、游客关注点统计</div>
            <div class="section-subtitle">高频提问内容 TOP10</div>
          </div>
          <div class="questions-list">
            <div
              v-for="(item, index) in reportData.topQuestions"
              :key="index"
              class="question-item"
            >
              <div class="question-rank" :class="{ top: index < 3 }">{{ index + 1 }}</div>
              <div class="question-content">
                <div class="question-text">{{ item.question || item.content || '暂无数据' }}</div>
                <div class="question-count">提问次数：{{ item.totalCount || 0 }} 次</div>
              </div>
            </div>
            <div v-if="!reportData.topQuestions.length" class="empty-state">
              <div class="empty-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M9.05 7a7.002 7.002 0 0 1 13.9 2" />
                  <path d="M9.05 17a7.002 7.002 0 0 0 13.9-2" />
                </svg>
              </div>
              <div class="empty-text">暂无数据</div>
            </div>
          </div>
        </div>

        <div class="section-card scenic-section">
          <div class="section-header">
            <div class="section-title">三、热门景点咨询</div>
            <div class="section-subtitle">景点咨询量 TOP5</div>
          </div>
          <div class="scenic-chart-wrapper">
            <div id="scenic-chart-report" class="scenic-chart"></div>
          </div>
        </div>
      </div>

      <div class="section-card complaint-section">
        <div class="section-header">
          <div class="section-title">四、投诉统计分析</div>
          <div class="section-subtitle">投诉工单集中问题处理情况</div>
          <div class="complaint-summary">
            <div class="summary-tag">
              <span class="tag-label">总投诉</span>
              <span class="tag-value">{{ formatNumber(totalComplaints) }}</span>
            </div>
            <div class="summary-tag pending">
              <span class="tag-label">待处理</span>
              <span class="tag-value">{{ formatNumber(totalPending) }}</span>
            </div>
            <div class="summary-tag processing">
              <span class="tag-label">处理中</span>
              <span class="tag-value">{{ formatNumber(totalProcessing) }}</span>
            </div>
          </div>
        </div>
        <div class="complaint-table">
          <div class="table-header">
            <div class="th rank">排名</div>
            <div class="th type">投诉类型</div>
            <div class="th count">投诉数量</div>
            <div class="th pending">待处理</div>
            <div class="th processing">处理中</div>
          </div>
          <div class="table-body">
            <div
              v-for="(item, index) in reportData.complaintStats"
              :key="index"
              class="table-row"
            >
              <div class="td rank">{{ index + 1 }}</div>
              <div class="td type">{{ item.complaintType || '未知类型' }}</div>
              <div class="td count">{{ formatNumber(item.count || 0) }}</div>
              <div class="td pending">{{ formatNumber(item.pendingCount || 0) }}</div>
              <div class="td processing">{{ formatNumber(item.processingCount || 0) }}</div>
            </div>
            <div v-if="!reportData.complaintStats.length" class="empty-row">
              <div class="empty-text">暂无投诉数据</div>
            </div>
          </div>
        </div>
      </div>

      <div class="section-card ai-section">
        <div class="section-header">
          <div class="section-title">五、AI 服务优化建议</div>
          <div class="section-subtitle">基于情感分析、高频关注点和投诉数据综合生成</div>
        </div>
        <div class="ai-content">
          <div v-if="!aiSuggestions" class="ai-placeholder">
            <div class="placeholder-illustration">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <div class="placeholder-text">点击下方按钮，AI 将基于当前数据为您生成专业的服务优化建议</div>
          </div>
          <div v-else class="ai-suggestions">
            <div class="suggestions-header">
              <div class="suggestions-badge">AI 生成建议</div>
              <div class="suggestions-date">生成时间：{{ new Date().toLocaleString('zh-CN') }}</div>
            </div>
            <div class="suggestions-content">
              {{ aiSuggestions }}
            </div>
          </div>
          <div class="ai-action">
            <el-button
              type="primary"
              :class="['ai-button', { loading: aiLoading }]"
              @click="fetchAiSuggestions"
              :loading="aiLoading"
            >
              <span v-if="aiLoading">
                <el-icon class="is-loading">
                  <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg" fill="currentColor">
                    <path d="M512 64c247.424 0 448 200.576 448 448S759.424 960 512 960 64 759.424 64 512 264.576 64 512 64zm0 82.133c-201.77 0-365.867 164.097-365.867 365.867S310.23 877.867 512 877.867 877.867 713.77 877.867 512 713.77 146.133 512 146.133z"/>
                  </svg>
                </el-icon>
                AI 分析生成中...
              </span>
              <span v-else>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="ai-icon">
                  <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                一键生成服务建议
              </span>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report-page {
  padding: 24px;
  background: #f5f7f5;
  min-height: 100vh;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.report-title-section {
  flex: 1;
}

.report-title {
  font-size: 24px;
  font-weight: 700;
  color: #123019;
  letter-spacing: 0.5px;
}

.report-subtitle {
  margin-top: 6px;
  font-size: 14px;
  color: rgba(18, 48, 25, 0.6);
}

.report-meta {
  display: flex;
  gap: 24px;
  margin-top: 12px;
}

.meta-item {
  font-size: 13px;
  color: rgba(18, 48, 25, 0.5);
}

.report-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.period-tabs {
  display: flex;
  gap: 8px;
}

.period-btn {
  padding: 8px 16px;
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

.export-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 600;
  background: linear-gradient(90deg, #1b5e20, #2e7d32);
  border: none;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.export-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(27, 94, 32, 0.3);
}

.export-icon {
  width: 16px;
  height: 16px;
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  background: #ffffff;
  border: 1px solid rgba(18, 48, 25, 0.08);
  border-radius: 16px;
  overflow: hidden;
}

.section-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(18, 48, 25, 0.06);
  background: linear-gradient(180deg, rgba(18, 48, 25, 0.02) 0%, transparent 100%);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #123019;
}

.section-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: rgba(18, 48, 25, 0.5);
}

.section-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.emotion-content {
  display: flex;
  padding: 24px;
  gap: 30px;
}

.emotion-chart-wrapper {
  flex: 0 0 420px;
}

.emotion-chart {
  width: 100%;
  height: 300px;
}

.emotion-summary {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  font-size: 13px;
  color: rgba(18, 48, 25, 0.6);
}

.summary-bar {
  display: flex;
  height: 8px;
  background: #f3f4f6;
  border-radius: 4px;
  overflow: hidden;
}

.bar-segment {
  height: 100%;
  transition: width 0.5s ease;
}

.bar-segment.satisfied {
  background: #1b5e20;
}

.bar-segment.normal {
  background: #f59e0b;
}

.bar-segment.dissatisfied {
  background: #ef4444;
}

.emotion-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.emotion-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(18, 48, 25, 0.02);
  border-radius: 12px;
}

.emotion-card .emotion-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
}

.emotion-card.satisfied .emotion-icon {
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
}

.emotion-card.normal .emotion-icon {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
}

.emotion-card.dissatisfied .emotion-icon {
  background: linear-gradient(135deg, #fee2e2, #fecaca);
}

.emotion-info {
  flex: 1;
}

.emotion-label {
  font-size: 12px;
  color: rgba(18, 48, 25, 0.5);
}

.emotion-value {
  font-size: 24px;
  font-weight: 700;
  color: #123019;
  margin-top: 4px;
}

.emotion-percent {
  font-size: 12px;
  color: #1b5e20;
  margin-top: 2px;
}

.complaint-summary {
  display: flex;
  gap: 16px;
  margin-top: 12px;
}

.summary-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(18, 48, 25, 0.04);
  border-radius: 999px;
}

.summary-tag .tag-label {
  font-size: 12px;
  color: rgba(18, 48, 25, 0.6);
}

.summary-tag .tag-value {
  font-size: 14px;
  font-weight: 600;
  color: #123019;
}

.summary-tag.pending {
  background: rgba(239, 68, 68, 0.08);
}

.summary-tag.pending .tag-value {
  color: #ef4444;
}

.summary-tag.processing {
  background: rgba(245, 158, 11, 0.08);
}

.summary-tag.processing .tag-value {
  color: #f59e0b;
}

.questions-list {
  padding: 20px;
}

.question-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: rgba(18, 48, 25, 0.02);
  border-radius: 10px;
  margin-bottom: 10px;
  transition: background 0.2s;
}

.question-item:hover {
  background: rgba(18, 48, 25, 0.05);
}

.question-rank {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 13px;
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
  font-size: 14px;
  color: #123019;
  line-height: 1.5;
}

.question-count {
  font-size: 12px;
  color: rgba(18, 48, 25, 0.5);
  margin-top: 6px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
}

.empty-icon {
  width: 48px;
  height: 48px;
  color: rgba(18, 48, 25, 0.3);
  margin-bottom: 12px;
}

.empty-text {
  font-size: 13px;
  color: rgba(18, 48, 25, 0.4);
}

.scenic-chart-wrapper {
  padding: 20px;
}

.scenic-chart {
  width: 100%;
  height: 340px;
}

.complaint-table {
  padding: 0 24px 24px;
}

.table-header {
  display: flex;
  padding: 12px 16px;
  background: rgba(18, 48, 25, 0.04);
  border-radius: 8px;
  margin-bottom: 8px;
}

.th {
  font-size: 13px;
  font-weight: 600;
  color: rgba(18, 48, 25, 0.7);
}

.th.rank {
  width: 60px;
}

.th.type {
  flex: 2;
}

.th.count,
.th.pending,
.th.processing {
  width: 100px;
  text-align: right;
}

.table-body {
  border: 1px solid rgba(18, 48, 25, 0.06);
  border-radius: 8px;
  overflow: hidden;
}

.table-row {
  display: flex;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(18, 48, 25, 0.04);
  transition: background 0.2s;
}

.table-row:hover {
  background: rgba(18, 48, 25, 0.02);
}

.table-row:last-child {
  border-bottom: none;
}

.td {
  font-size: 13px;
  color: #123019;
}

.td.rank {
  width: 60px;
  color: rgba(18, 48, 25, 0.5);
}

.td.type {
  flex: 2;
}

.td.count,
.td.pending,
.td.processing {
  width: 100px;
  text-align: right;
}

.td.pending {
  color: #ef4444;
}

.td.processing {
  color: #f59e0b;
}

.empty-row {
  padding: 40px;
  text-align: center;
}

.ai-content {
  padding: 24px;
}

.ai-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}

.placeholder-illustration {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1b5e20;
  margin-bottom: 20px;
}

.placeholder-illustration svg {
  width: 40px;
  height: 40px;
}

.placeholder-text {
  font-size: 14px;
  color: rgba(18, 48, 25, 0.5);
  text-align: center;
  max-width: 400px;
}

.ai-suggestions {
  margin-bottom: 24px;
}

.suggestions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.suggestions-badge {
  padding: 6px 14px;
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  color: #1b5e20;
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
}

.suggestions-date {
  font-size: 12px;
  color: rgba(18, 48, 25, 0.5);
}

.suggestions-content {
  background: rgba(18, 48, 25, 0.02);
  border: 1px solid rgba(18, 48, 25, 0.06);
  border-radius: 12px;
  padding: 24px;
  line-height: 1.8;
  color: #123019;
  font-size: 14px;
  white-space: pre-wrap;
}

.ai-action {
  text-align: center;
}

.ai-button {
  padding: 14px 40px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 14px;
  background: linear-gradient(90deg, #1b5e20, #2e7d32);
  border: none;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  transition: all 0.3s;
}

.ai-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(27, 94, 32, 0.3);
}

.ai-icon {
  width: 18px;
  height: 18px;
}

@media (max-width: 1200px) {
  .section-row {
    grid-template-columns: 1fr;
  }
  
  .emotion-content {
    flex-direction: column;
  }
  
  .emotion-chart-wrapper {
    flex: none;
    width: 100%;
  }
  
  .emotion-cards {
    grid-template-columns: 1fr;
  }
  
  .report-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .report-actions {
    justify-content: space-between;
  }
}
</style>
