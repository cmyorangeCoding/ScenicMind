<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const query = reactive({
  keyword: ''
})

const tableLoading = ref(false)
const rows = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const parsePage = (payload) => {
  const data = payload?.data || payload
  const records = data?.records || data?.list || data?.items || []
  const t = Number(data?.total ?? data?.count ?? records.length)
  return { records, total: Number.isFinite(t) ? t : records.length }
}

const fetchList = async () => {
  tableLoading.value = true
  try {
    const res = await http.get('/admin/avatar/page', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: query.keyword || undefined
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

const onReset = () => {
  query.keyword = ''
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
  avatarId: null,
  avatarName: '',
  avatarImage: null,
  vcn: '',
  speed: 50,
  pitch: 50,
  volume: 50,
  status: 1
})
const avatarPreview = ref('')

const vcnOptions = [
  { label: '温柔男声', value: 'x6_wenrounansheng_mini' },
  { label: '甜美女声', value: 'x4_panting' },
  { label: '干练爽朗女声', value: 'x6_jingqudaolannvsheng_mini' },
  { label: '古风侠女', value: 'x6_gufengxianv_mini' },
  { label: '女生英语', value: 'x4_EnUs_Catherine_profnews' },
  { label: '拟人童声', value: 'x5_lingyouyou_flow' }
]

const rules = {
  avatarId: [{ required: true, message: '请输入数字人ID', trigger: 'blur' }],
  avatarName: [{ required: true, message: '请输入数字人名称', trigger: 'blur' }],
  vcn: [{ required: true, message: '请选择语音音色', trigger: 'change' }]
}

const resetForm = () => {
  form.avatarId = null
  form.avatarName = ''
  form.avatarImage = null
  form.vcn = ''
  form.speed = 50
  form.pitch = 50
  form.volume = 50
  form.status = 1
  avatarPreview.value = ''
}

const openAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  dialogOpen.value = true
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  form.avatarId = row?.avatarId || null
  form.avatarName = row?.avatarName || ''
  form.avatarImage = null
  form.vcn = row?.vcn || ''
  form.speed = row?.speed ?? 50
  form.pitch = row?.pitch ?? 50
  form.volume = row?.volume ?? 50
  form.status = row?.status ?? 1
  // 如果有已有头像，直接显示
  avatarPreview.value = row?.avatarImage || ''
  dialogOpen.value = true
}

const handleImageChange = (uploadFile) => {
  form.avatarImage = uploadFile.raw
  if (uploadFile.raw && window.URL && URL.createObjectURL) {
    avatarPreview.value = URL.createObjectURL(uploadFile.raw)
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  dialogLoading.value = true
  try {
    const formData = new FormData()
    // avatarId 和 avatarName 两个都必须传
    formData.append('avatarId', form.avatarId)
    formData.append('avatarName', form.avatarName)
    formData.append('vcn', form.vcn)
    formData.append('speed', String(form.speed))
    formData.append('pitch', String(form.pitch))
    formData.append('volume', String(form.volume))
    formData.append('status', String(form.status))

    if (form.avatarImage) {
      formData.append('avatarImage', form.avatarImage)
    }

    // 注意：FormData 不要手动设置 Content-Type，浏览器会自动处理 boundary
    let res
    if (dialogMode.value === 'add') {
      res = await http.post('/admin/avatar', formData)
    } else {
      res = await http.put('/admin/avatar', formData)
    }

    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || '提交失败')

    ElMessage.success(dialogMode.value === 'add' ? '新增成功' : '更新成功')
    dialogOpen.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    dialogLoading.value = false
  }
}

const toggleStatus = async (row) => {
  const id = row?.avatarId
  const currentStatus = row?.status
  if (!id) return

  const newStatus = currentStatus === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定${action}该数字人？`, action, {
      type: newStatus === 1 ? 'success' : 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.put(`/admin/avatar/${id}/status`, null, {
      params: { status: newStatus }
    })
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || `${action}失败`)
    ElMessage.success(`${action}成功`)
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || `${action}失败`)
  }
}

const removeRow = async (row) => {
  const id = row?.avatarId
  if (!id) return

  try {
    await ElMessageBox.confirm('确定删除该数字人？此操作不可恢复。', '删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.delete(`/admin/avatar/${id}`)
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || '删除失败')
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">数字人管理</div>
      <div class="page-sub">管理景区数字人形象，支持添加、编辑、启用/禁用、删除操作</div>
    </div>

    <el-card class="card" shadow="never">
      <div class="toolbar">
        <div class="filters">
          <span class="label">关键词</span>
          <el-input v-model="query.keyword" placeholder="名称/音色搜索" style="width: 200px" clearable @keyup.enter="onSearch" />

          <el-button type="primary" class="btn" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>

        <div class="actions">
          <el-button type="primary" class="btn-add" @click="openAdd">新增数字人</el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="tableLoading" class="table" stripe>
        <el-table-column label="头像" width="100" align="center">
          <template #default="{ row }">
            <el-avatar v-if="row?.avatarImage" :src="row.avatarImage" :size="48" shape="square" />
            <div v-else class="avatar-placeholder">无</div>
          </template>
        </el-table-column>
        <el-table-column label="名称" prop="avatarName" min-width="140" />
        <el-table-column label="语音音色" min-width="160">
          <template #default="{ row }">
            {{ vcnOptions.find(v => v.value === row?.vcn)?.label || row?.vcn || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="语速" width="80" align="center">
          <template #default="{ row }">
            <span class="param-badge">{{ row?.speed ?? 50 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="语调" width="80" align="center">
          <template #default="{ row }">
            <span class="param-badge">{{ row?.pitch ?? 50 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="音量" width="80" align="center">
          <template #default="{ row }">
            <span class="param-badge">{{ row?.volume ?? 50 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row?.status === 1 ? 'success' : 'danger'" size="small">
              {{ row?.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row?.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row?.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
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
          :page-sizes="[5, 10, 15, 20]"
          :total="total"
          @size-change="onSizeChange"
          @current-change="onCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogOpen" :title="dialogMode === 'add' ? '新增数字人' : '编辑数字人'" width="680px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
        <el-form-item label="数字人ID" prop="avatarId">
          <el-input v-model="form.avatarId" placeholder="请输入数字人ID" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="数字人名称" prop="avatarName">
          <el-input v-model="form.avatarName" placeholder="请输入数字人名称" />
        </el-form-item>

        <el-form-item label="语音音色" prop="vcn">
          <el-select v-model="form.vcn" placeholder="请选择语音音色" style="width: 100%">
            <el-option v-for="item in vcnOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <div class="param-section">
          <div class="param-title">语音参数</div>
          <div class="param-grid">
            <el-form-item label="语速">
              <el-input-number v-model="form.speed" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
            
            <el-form-item label="语调">
              <el-input-number v-model="form.pitch" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
            
            <el-form-item label="音量">
              <el-input-number v-model="form.volume" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </div>
        </div>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="头像图片">
          <el-upload
            class="avatar-uploader"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleImageChange"
            accept="image/*"
          >
            <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview" />
            <div v-else class="upload-placeholder">
              <span class="upload-icon">+</span>
              <span class="upload-text">上传头像</span>
            </div>
          </el-upload>
          <div class="upload-hint">支持 JPG、PNG 格式，建议尺寸 200x200</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="submitForm">
          {{ dialogMode === 'add' ? '新增' : '保存' }}
        </el-button>
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

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

.avatar-placeholder {
  width: 48px;
  height: 48px;
  background: rgba(18, 48, 25, 0.08);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(18, 48, 25, 0.4);
  font-size: 12px;
}

.param-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 10px;
  background: rgba(18, 48, 25, 0.06);
  color: rgba(18, 48, 25, 0.7);
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.param-section {
  background: rgba(18, 48, 25, 0.03);
  border: 1px solid rgba(18, 48, 25, 0.08);
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 18px;
}

.param-title {
  font-size: 14px;
  font-weight: 600;
  color: #123019;
  margin-bottom: 14px;
}

.param-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.param-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.param-label {
  font-size: 13px;
  color: rgba(18, 48, 25, 0.8);
  font-weight: 500;
}

.param-control {
  display: flex;
  align-items: center;
  gap: 6px;
}

.param-btn {
  width: 32px;
  height: 32px;
  border: 1px solid rgba(18, 48, 25, 0.15);
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  color: rgba(18, 48, 25, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .2s;
}

.param-btn:hover {
  background: rgba(18, 48, 25, 0.06);
  border-color: rgba(18, 48, 25, 0.25);
  color: #123019;
}

.param-input {
  width: 80px;
}

.param-input :deep(.el-input__wrapper) {
  padding: 0 8px;
}

.param-hint {
  font-size: 11px;
  color: rgba(18, 48, 25, 0.45);
}

.avatar-uploader {
  width: 120px;
  height: 120px;
  cursor: pointer;
}

.upload-placeholder {
  width: 120px;
  height: 120px;
  border: 2px dashed rgba(18, 48, 25, 0.2);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all .2s;
}

.upload-placeholder:hover {
  border-color: rgba(18, 48, 25, 0.4);
  background: rgba(18, 48, 25, 0.02);
}

.upload-icon {
  font-size: 28px;
  color: rgba(18, 48, 25, 0.3);
}

.upload-text {
  font-size: 12px;
  color: rgba(18, 48, 25, 0.5);
}

.avatar-preview {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid rgba(18, 48, 25, 0.1);
}

.upload-hint {
  margin-top: 8px;
  font-size: 11px;
  color: rgba(18, 48, 25, 0.45);
}

@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .actions {
    justify-content: flex-end;
  }
  .param-grid {
    grid-template-columns: 1fr;
  }
}
</style>
