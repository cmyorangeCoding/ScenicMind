<template>
  <div class="complaint-manage-container">
    <div class="page-header">
      <div class="header-title">
        <h1>投诉工单管理</h1>
        <p>处理游客投诉工单，跟进服务质量</p>
      </div>
    </div>

    <div class="search-filter-card">
      <div class="search-row">
        <div class="search-item">
          <label>处理状态</label>
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable @change="handleSearch">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已办结" :value="2" />
          </el-select>
        </div>
        <div class="search-item">
          <label>投诉时间</label>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleSearch"
          />
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="tableData" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="createTime" label="投诉时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="complaintType" label="投诉类型" width="120" />
        <el-table-column prop="content" label="投诉内容" min-width="200">
          <template #default="{ row }">
            <span class="content-text">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userPhone" label="联系方式" min-width="180">
          <template #default="{ row }">
            <div class="contact-info">
              <div class="phone-text">{{ row.userPhone || row.phone || '-' }}</div>
              <el-button type="primary" link size="small" @click="goToUserManage(row.userPhone || row.phone)">
                查看对话
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="statusText" label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="replyContent" label="处理回复" min-width="150">
          <template #default="{ row }">
            <span class="reply-text">{{ row.replyContent || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleComplaint(row)" v-if="row.status !== 2">
              处理
            </el-button>
            <el-button type="success" link size="small" @click="markDone(row)" v-if="row.status !== 2">
              办结
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </div>

    <el-dialog
      v-model="handleDialogVisible"
      title="处理投诉"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="handleForm" label-width="80px">
        <el-form-item label="投诉内容">
          <el-input
              :model-value="currentComplaint?.content"
              type="textarea"
              :rows="3"
              disabled
            />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="handleForm.status" placeholder="请选择">
            <el-option label="处理中" :value="1" />
            <el-option label="已办结" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理回复">
          <el-input
            v-model="handleForm.replyContent"
            type="textarea"
            :rows="4"
            placeholder="请输入处理回复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle" :loading="handleLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import http from '../api/http'

const router = useRouter()

const loading = ref(false)
const handleLoading = ref(false)
const handleDialogVisible = ref(false)
const dateRange = ref([])
const tableData = ref([])
const currentComplaint = ref(null)

const searchForm = reactive({
  status: null,
  startTime: '',
  endTime: ''
})

const handleForm = reactive({
  id: null,
  status: 1,
  replyContent: ''
})

const pagination = reactive({
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

const getStatusType = (status) => {
  if (status === 0) return 'danger'
  if (status === 1) return 'warning'
  if (status === 2) return 'success'
  return 'info'
}

const handleSearch = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (searchForm.status !== null && searchForm.status !== undefined && searchForm.status !== '') {
      params.status = searchForm.status
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    console.log('请求参数:', params)
    const response = await http.get('/admin/complaint-manage/page', { params })
    console.log('响应数据:', response)
    if (response.data?.code === 200 && response.data.data?.records?.length > 0) {
      console.log('第一条数据:', response.data.data.records[0])
      console.log('userPhone字段:', response.data.data.records[0].userPhone)
    }
    
    if (response.data?.code === 200) {
      tableData.value = response.data.data?.records || []
      pagination.total = response.data.data?.total || 0
    } else {
      ElMessage.error(response.data?.message || '查询失败')
    }
  } catch (error) {
    console.error('查询失败 - 错误对象:', error)
    console.error('响应数据:', error?.response)
    ElMessage.error(error?.response?.data?.message || error?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.status = null
  dateRange.value = []
  pagination.pageNum = 1
  handleSearch()
}

const handleComplaint = (row) => {
  currentComplaint.value = row
  handleForm.id = row.id
  handleForm.status = 1
  handleForm.replyContent = row.replyContent || ''
  handleDialogVisible.value = true
}

const markDone = async (row) => {
  try {
    await ElMessageBox.confirm('确认将该投诉标记为已办结吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    handleLoading.value = true
    const response = await http.put('/admin/complaint-manage/handle', null, {
      params: {
        id: row.id,
        status: 2,
        replyContent: row.replyContent || ''
      }
    })
    if (response.data?.code === 200) {
      ElMessage.success('办结成功')
      handleSearch()
    } else {
      ElMessage.error(response.data?.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error(error?.response?.data?.message || error?.message || '操作失败')
    }
  } finally {
    handleLoading.value = false
  }
}

const submitHandle = async () => {
  if (!handleForm.replyContent && handleForm.status === 2) {
    ElMessage.warning('请输入处理回复内容')
    return
  }
  handleLoading.value = true
  try {
    const response = await http.put('/admin/complaint-manage/handle', null, {
      params: {
        id: handleForm.id,
        status: handleForm.status,
        replyContent: handleForm.replyContent
      }
    })
    if (response.data?.code === 200) {
      ElMessage.success('处理成功')
      handleDialogVisible.value = false
      handleSearch()
    } else {
      ElMessage.error(response.data?.message || '处理失败')
    }
  } catch (error) {
    console.error('处理失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '处理失败')
  } finally {
    handleLoading.value = false
  }
}

const goToUserManage = (phone) => {
  router.push({
    path: '/admin/user-manage',
    query: { phone }
  })
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.complaint-manage-container {
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

.search-filter-card {
  background: white;
  padding: 24px 32px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.search-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.search-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-item label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.search-item :deep(.el-select) {
  width: 180px;
}

.search-actions {
  display: flex;
  gap: 12px;
}

.table-card {
  background: white;
  padding: 24px 32px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.content-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.reply-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #666;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.phone-text {
  font-weight: 500;
  color: #333;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
