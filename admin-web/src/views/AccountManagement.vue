<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

// 后端 DTO 入参（你给的代码）：
// - AdminAddDTO:    { realName, account, password, phone }
// - AdminUpdateDTO: { adminId, realName, phone }
// - AdminStatusDTO: { adminId, status }

const query = reactive({
  realName: ''
})

const tableLoading = ref(false)
const rows = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const pick = (row, keys, fallback = '') => {
  for (const k of keys) {
    if (row && row[k] !== undefined && row[k] !== null && row[k] !== '') return row[k]
  }
  return fallback
}

// AdminVO 时间字段：lastOperateTime/createTime 是 LocalDateTime
// 这里做一个轻量格式化（后端若已格式化成字符串，也能直接显示）
const formatTime = (v) => {
  if (!v) return '-'
  if (typeof v === 'string') {
    const s = v.replace('T', ' ')
    return s.length >= 19 ? s.slice(0, 19) : s
  }
  try {
    const d = new Date(v)
    if (Number.isNaN(d.getTime())) return String(v)
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return String(v)
  }
}

const parsePage = (payload) => {
  const data = payload?.data || payload
  const records = data?.records || data?.list || data?.items || []
  const t = Number(data?.total ?? data?.count ?? records.length)
  return { records, total: Number.isFinite(t) ? t : records.length }
}

const fetchList = async () => {
  tableLoading.value = true
  try {
    const res = await http.get('/admin/account/page', {
      params: {
        realName: query.realName || undefined,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true || Boolean(result?.data)
    if (!ok) throw new Error(result?.msg || result?.message || '查询失败')

    const parsed = parsePage(result)
    rows.value = parsed.records
    total.value = parsed.total
  } catch (e) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    tableLoading.value = false
  }
}

const onSearch = () => {
  pageNum.value = 1
  fetchList()
}

const onSizeChange = (v) => {
  pageSize.value = v
  pageNum.value = 1
  fetchList()
}

const onCurrentChange = (v) => {
  pageNum.value = v
  fetchList()
}

const dialogOpen = ref(false)
const dialogMode = ref('add')
const dialogLoading = ref(false)
const formRef = ref(null)
const form = reactive({
  adminId: null,
  realName: '',
  account: '',
  phone: '',
  password: ''
})

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const openAdd = () => {
  dialogMode.value = 'add'
  form.adminId = null
  form.realName = ''
  form.account = ''
  form.phone = ''
  form.password = ''
  dialogOpen.value = true
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  form.adminId = pick(row, ['adminId', 'id'], null)
  form.realName = pick(row, ['realName', 'name'], '')
  form.account = pick(row, ['account', 'username', 'loginName'], '')
  form.phone = pick(row, ['phone', 'mobile', 'phoneNumber'], '')
  form.password = ''
  dialogOpen.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (dialogMode.value === 'add' && !String(form.password || '').trim()) {
    ElMessage.error('请输入初始密码')
    return
  }

  dialogLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      const res = await http.post('/admin/account/add', {
        realName: form.realName,
        account: form.account,
        password: form.password,
        phone: form.phone
      })
      const result = res?.data
      const ok = result?.code === 1 || result?.code === 200 || result?.success === true
      if (!ok) throw new Error(result?.msg || result?.message || '添加失败')
      ElMessage.success('添加成功')
    } else {
      const res = await http.put('/admin/account/update', {
        adminId: form.adminId,
        realName: form.realName,
        phone: form.phone
      })
      const result = res?.data
      const ok = result?.code === 1 || result?.code === 200 || result?.success === true
      if (!ok) throw new Error(result?.msg || result?.message || '修改失败')
      ElMessage.success('修改成功')
    }
    dialogOpen.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    dialogLoading.value = false
  }
}

const toggleStatus = async (row) => {
  const adminId = pick(row, ['adminId', 'id'], null)
  const currentStatus = pick(row, ['status'], 1)
  const nextStatus = String(currentStatus) === '1' ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定${actionText}该账号？`, `${actionText}账号`, {
      type: 'warning',
      confirmButtonText: actionText,
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.put('/admin/account/status', { adminId, status: nextStatus })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || '状态修改失败')
    ElMessage.success('状态修改成功')
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '状态修改失败')
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">账号管理</div>
      <div class="page-sub">管理员账号的查询、维护、启用/禁用</div>
    </div>

    <el-card class="card" shadow="never">
      <div class="toolbar">
        <div class="filters">
          <span class="label">姓名</span>
          <el-input v-model="query.realName" placeholder="请输入管理员姓名" style="width: 220px" clearable />
          <el-button type="primary" class="btn" @click="onSearch">查询</el-button>
        </div>
        <el-button type="primary" class="btn-add" @click="openAdd">添加管理员</el-button>
      </div>

      <el-table :data="rows" v-loading="tableLoading" class="table" stripe>
        <el-table-column label="姓名" min-width="120">
          <template #default="{ row }">
            {{ pick(row, ['realName', 'name'], '-') }}
          </template>
        </el-table-column>
        <el-table-column label="账号" min-width="160">
          <template #default="{ row }">
            {{ pick(row, ['account', 'username', 'loginName'], '-') }}
          </template>
        </el-table-column>

        <el-table-column label="手机号" min-width="160">
          <template #default="{ row }">
            {{ pick(row, ['phone', 'mobile', 'phoneNumber'], '-') }}
          </template>
        </el-table-column>
        <el-table-column label="账号状态" min-width="120">
          <template #default="{ row }">
            <span class="status-dot" :class="String(pick(row, ['status'], 1)) === '1' ? 'on' : 'off'" />
            <span class="status-text">
              {{ pick(row, ['statusText'], '') || (String(pick(row, ['status'], 1)) === '1' ? '启用' : '禁用') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="最后操作时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(pick(row, ['lastOperateTime', 'updateTime', 'updatedAt', 'lastUpdateTime'], '')) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button link type="danger" @click="toggleStatus(row)">{{ String(pick(row, ['status'], 1)) === '1' ? '禁用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <div class="total">共 {{ total }} 条</div>
        <el-pagination
          background
          layout="sizes, prev, pager, next, jumper"
          :current-page="pageNum"
          :page-size="pageSize"
          :page-sizes="[5, 10, 15]"
          :total="total"
          @size-change="onSizeChange"
          @current-change="onCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogOpen" :title="dialogMode === 'add' ? '添加管理员' : '修改管理员'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" :disabled="dialogMode === 'edit'" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'add'" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-head {
  margin-bottom: 14px;
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

.toolbar {
  display: flex;
  align-items: center;
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

.btn-add {
  background: linear-gradient(90deg, #f0c000, #f3cf3a);
  border: none;
  color: #2b2b2b;
  font-weight: 700;
}

.table {
  border-radius: 10px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

.status-dot.on {
  background: #22c55e;
}

.status-dot.off {
  background: #ef4444;
}

.status-text {
  color: rgba(18, 48, 25, 0.72);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.total {
  color: rgba(18, 48, 25, 0.62);
  font-size: 13px;
}
</style>
