<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const query = reactive({
  startDate: '',
  endDate: ''
})

const hotLoading = ref(false)
const hotRows = ref([])

const pendingLoading = ref(false)
const pendingRows = ref([])

const fetchHotQuestions = async () => {
  hotLoading.value = true
  try {
    const res = await http.get('/admin/hot-question/list', {
      params: {
        startDate: query.startDate || undefined,
        endDate: query.endDate || undefined
      }
    })

    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true || Boolean(result?.data)
    if (!ok) throw new Error(result?.msg || result?.message || '查询失败')

    hotRows.value = result?.data || []
  } catch (e) {
    ElMessage.error(e?.message || '查询热门问题失败')
  } finally {
    hotLoading.value = false
  }
}

const fetchPendingCaches = async () => {
  pendingLoading.value = true
  try {
    const res = await http.get('/admin/hot-question/pending')

    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true || Boolean(result?.data)
    if (!ok) throw new Error(result?.msg || result?.message || '查询失败')

    pendingRows.value = result?.data || []
  } catch (e) {
    ElMessage.error(e?.message || '查询待审核缓存失败')
  } finally {
    pendingLoading.value = false
  }
}

const onSearch = () => {
  fetchHotQuestions()
}

const onReset = () => {
  query.startDate = ''
  query.endDate = ''
  fetchHotQuestions()
}

const removeHotQuestion = async (row) => {
  const question = row?.question
  if (!question) return

  try {
    await ElMessageBox.confirm('确定删除该热门问题统计记录？', '删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.delete('/admin/hot-question', {
      params: { question }
    })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || '删除失败')
    ElMessage.success('删除成功')
    fetchHotQuestions()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const approveCache = async (row) => {
  const question = row?.question
  if (!question) return

  try {
    await ElMessageBox.confirm('确定审核通过该缓存？通过后将用于前端语义匹配。', '审核通过', {
      type: 'success',
      confirmButtonText: '通过',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.post('/admin/hot-question/approve', null, {
      params: { question }
    })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || '审核失败')
    ElMessage.success('审核通过成功')
    fetchPendingCaches()
  } catch (e) {
    ElMessage.error(e?.message || '审核失败')
  }
}

const rejectCache = async (row) => {
  const question = row?.question
  if (!question) return

  try {
    await ElMessageBox.confirm('确定拒绝该缓存？将从向量库中删除。', '拒绝审核', {
      type: 'warning',
      confirmButtonText: '拒绝',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.post('/admin/hot-question/reject', null, {
      params: { question }
    })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || '操作失败')
    ElMessage.success('拒绝成功')
    fetchPendingCaches()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

onMounted(() => {
  fetchHotQuestions()
  fetchPendingCaches()
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">用户高频问题管理</div>
      <div class="page-sub">管理游客高频提问统计和向量库缓存审核</div>
    </div>

    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <span class="icon">📊</span>
            <span>热门问答统计</span>
          </div>
          <div class="card-desc">展示游客高频提问数据统计</div>
        </div>
      </template>

      <div class="toolbar">
        <div class="filters">
          <span class="label">开始日期</span>
          <el-date-picker
            v-model="query.startDate"
            type="date"
            placeholder="请选择"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 150px"
          />

          <span class="label">结束日期</span>
          <el-date-picker
            v-model="query.endDate"
            type="date"
            placeholder="请选择"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 150px"
          />

          <el-button type="primary" class="btn" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>

      <el-table
        :data="hotRows"
        v-loading="hotLoading"
        class="table"
        stripe
      >
        <el-table-column type="index" label="排名" width="80" align="center">
          <template #default="{ $index }">
            <span class="rank-badge" :class="{ 'rank-top': $index < 3 }">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="问题内容" min-width="320">
          <template #default="{ row }">
            <div class="question-cell" :title="row?.question || ''">{{ row?.question || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="提问次数" width="120" align="center">
          <template #default="{ row }">
            <span class="count-num">{{ row?.totalCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeHotQuestion(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="card" shadow="never" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <span class="icon">💾</span>
            <span>向量库缓存审核</span>
          </div>
          <div class="card-desc">管理用于语义匹配的向量库缓存数据，审核通过后将用于前端问题匹配</div>
        </div>
      </template>

      <el-table
        :data="pendingRows"
        v-loading="pendingLoading"
        class="table"
        stripe
      >
        <el-table-column type="index" label="序号" width="80" align="center" />
        <el-table-column label="问题内容" min-width="240">
          <template #default="{ row }">
            <div class="question-cell" :title="row?.question || ''">{{ row?.question || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="答案内容" min-width="300">
          <template #default="{ row }">
            <div class="answer-cell" :title="row?.answer || ''">{{ row?.answer || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="success" @click="approveCache(row)">通过</el-button>
            <el-button link type="warning" @click="rejectCache(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!pendingLoading && pendingRows.length === 0" description="暂无待审核缓存数据" />
    </el-card>
  </div>
</template>

<style scoped>
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

.card {
  border: 1px solid rgba(18, 48, 25, 0.08);
  border-radius: 14px;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #123019;
}

.card-title .icon {
  font-size: 18px;
}

.card-desc {
  font-size: 12px;
  color: rgba(18, 48, 25, 0.55);
  margin-left: 26px;
}

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.label {
  color: rgba(18, 48, 25, 0.78);
  font-size: 13px;
}

.btn {
  background: linear-gradient(90deg, #1b5e20, #2e7d32);
  border: none;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(18, 48, 25, 0.08);
  color: rgba(18, 48, 25, 0.65);
  font-weight: 600;
  font-size: 13px;
}

.rank-badge.rank-top {
  background: linear-gradient(135deg, #f0c000, #f3cf3a);
  color: #2b2b2b;
  font-weight: 700;
}

.question-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.answer-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(18, 48, 25, 0.75);
}

.count-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 14px;
  background: linear-gradient(90deg, rgba(46, 125, 50, 0.1), rgba(56, 142, 60, 0.1));
  color: #2e7d32;
  border-radius: 999px;
  font-weight: 700;
  font-size: 14px;
}

@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
