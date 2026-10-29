<template>
  <div class="user-manage-container">
    <div class="page-header">
      <div class="header-title">
        <h1>用户数据管理</h1>
        <p>查看游客用户信息和历史对话记录</p>
      </div>
    </div>

    <div class="search-card">
      <div class="search-row">
        <div class="search-item">
          <label>游客手机号</label>
          <el-input
            v-model="searchPhone"
            placeholder="请输入游客手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="searching">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="userInfo" class="user-info-card">
      <div class="section-title">用户信息</div>
      <div class="info-grid">
        <div class="info-item">
          <span class="label">用户ID</span>
          <span class="value">{{ userInfo.userId }}</span>
        </div>
        <div class="info-item">
          <span class="label">手机号</span>
          <span class="value">{{ userInfo.phone }}</span>
        </div>
        <div class="info-item">
          <span class="label">昵称</span>
          <span class="value">{{ userInfo.nickname || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">创建时间</span>
          <span class="value">{{ formatDateTime(userInfo.createTime) }}</span>
        </div>
      </div>
    </div>

    <div v-if="userInfo" class="chat-history-card">
      <div class="section-header">
        <div class="section-title">历史对话记录</div>
        <div class="filter-row">
          <div class="filter-item">
            <label>情绪筛选</label>
            <el-select v-model="filterForm.emotion" placeholder="全部" clearable @change="handleSearchChat">
              <el-option label="满意" value="满意" />
              <el-option label="一般" value="一般" />
              <el-option label="不满" value="不满" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>时间范围</label>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="handleSearchChat"
            />
          </div>
        </div>
      </div>

      <el-table :data="chatHistoryData" stripe style="width: 100%" v-loading="loadingChat">
        <el-table-column prop="createTime" label="对话时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="question" label="用户问题" min-width="200">
          <template #default="{ row }">
            <span class="question-text">{{ row.question }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="answer" label="AI回答" min-width="200">
          <template #default="{ row }">
            <span class="answer-text">{{ row.answer }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="emotion" label="情绪" width="100">
          <template #default="{ row }">
            <el-tag :type="getEmotionType(row.emotion)" size="small">
              {{ row.emotion }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="chatPagination.pageNum"
          v-model:page-size="chatPagination.pageSize"
          :total="chatPagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearchChat"
          @current-change="handleSearchChat"
        />
      </div>
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      title="对话详情"
      width="700px"
    >
      <div class="detail-content">
        <div class="detail-item">
          <div class="detail-label">对话时间</div>
          <div class="detail-value">{{ formatDateTime(currentChat?.createTime) }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">情绪</div>
          <div class="detail-value">
            <el-tag :type="getEmotionType(currentChat?.emotion)" size="small">
              {{ currentChat?.emotion }}
            </el-tag>
          </div>
        </div>
        <div class="detail-item">
          <div class="detail-label">用户问题</div>
          <div class="detail-value detail-text">{{ currentChat?.question }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">AI回答</div>
          <div class="detail-value detail-text">{{ currentChat?.answer }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <div v-if="searched && !userInfo">
      <el-empty description="未找到该用户" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import http from '../api/http'

const route = useRoute()

const searching = ref(false)
const loadingChat = ref(false)
const searched = ref(false)
const detailDialogVisible = ref(false)
const dateRange = ref([])
const searchPhone = ref('')
const userInfo = ref(null)
const chatHistoryData = ref([])
const currentChat = ref(null)

const filterForm = reactive({
  emotion: ''
})

const chatPagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const getEmotionType = (emotion) => {
  if (emotion === '满意') return 'success'
  if (emotion === '一般') return 'warning'
  if (emotion === '不满') return 'danger'
  return 'info'
}

const handleSearch = async () => {
  if (!searchPhone.value) {
    ElMessage.warning('请输入手机号')
    return
  }
  searching.value = true
  searched.value = true
  try {
    const response = await http.get('/admin/user-manage/detail', {
      params: {
        phone: searchPhone.value,
        pageNum: 1,
        pageSize: chatPagination.pageSize
      }
    })
    if (response.data?.code === 200) {
      userInfo.value = response.data.data?.userInfo
      chatHistoryData.value = response.data.data?.chatHistoryPage?.records || []
      chatPagination.total = response.data.data?.chatHistoryPage?.total || 0
      chatPagination.pageNum = 1
    } else {
      ElMessage.error(response.data?.message || '查询失败')
      userInfo.value = null
      chatHistoryData.value = []
      chatPagination.total = 0
    }
  } catch (error) {
    console.error('查询失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '查询失败')
    userInfo.value = null
    chatHistoryData.value = []
    chatPagination.total = 0
  } finally {
    searching.value = false
  }
}

const handleSearchChat = async () => {
  if (!userInfo.value) return
  loadingChat.value = true
  try {
    const params = {
      phone: searchPhone.value,
      pageNum: chatPagination.pageNum,
      pageSize: chatPagination.pageSize
    }
    if (filterForm.emotion) {
      params.emotion = filterForm.emotion
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    const response = await http.get('/admin/user-manage/detail', { params })
    if (response.data?.code === 200) {
      chatHistoryData.value = response.data.data?.chatHistoryPage?.records || []
      chatPagination.total = response.data.data?.chatHistoryPage?.total || 0
    } else {
      ElMessage.error(response.data?.message || '查询失败')
    }
  } catch (error) {
    console.error('查询失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '查询失败')
  } finally {
    loadingChat.value = false
  }
}

const showDetail = (row) => {
  currentChat.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  if (route.query.phone) {
    searchPhone.value = route.query.phone
    handleSearch()
  }
})
</script>

<style scoped>
.user-manage-container {
  padding: 24px;
  background: linear-gradient(135deg, #f5f7f5 0%, #e8f5e9 100%);
  min-height: calc(100vh - 80px);
}

.page-header {
  background: white;
  padding: 24px 32px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-header .header-title h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #123019;
  font-weight: 600;
}

.page-header .header-title p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.search-card {
  background: white;
  padding: 24px 32px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.search-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.search-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  max-width: 400px;
}

.search-item label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.user-info-card, .chat-history-card {
  background: white;
  padding: 24px 32px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #123019;
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-item label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.filter-item :deep(.el-select) {
  width: 180px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item .label {
  font-size: 14px;
  color: #666;
}

.info-item .value {
  font-size: 16px;
  font-weight: 600;
  color: #123019;
}

.question-text, .answer-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.detail-content {
  padding: 16px 0;
}

.detail-item {
  margin-bottom: 20px;
}

.detail-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.detail-value {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.detail-value.detail-text {
  background: #f5f7f5;
  padding: 12px 16px;
  border-radius: 8px;
  white-space: pre-wrap;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
