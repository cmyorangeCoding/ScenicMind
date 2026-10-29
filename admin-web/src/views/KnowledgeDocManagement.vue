<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

// 通用知识库管理
// 对接后端：/admin/knowledge
// - GET    /page           分页查询（category, tag, keyword）
// - POST   /               新增（KnowledgeDocDTO）
// - PUT    /               编辑（KnowledgeDocDTO）
// - DELETE /{id}           删除
// - POST   /batch-delete   批量删除（List<Long> ids）

const query = reactive({
  category: '',
  tag: '',
  keyword: ''
})

const tableLoading = ref(false)
const rows = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref([])

const parsePage = (payload) => {
  const data = payload?.data || payload
  const records = data?.records || data?.list || data?.items || []
  const t = Number(data?.total ?? data?.count ?? records.length)
  return { records, total: Number.isFinite(t) ? t : records.length }
}

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

const fetchList = async () => {
  tableLoading.value = true
  try {
    const res = await http.get('/admin/knowledge/page', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        category: query.category || undefined,
        tag: query.tag || undefined,
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

const onSizeChange = (v) => {
  pageSize.value = v
  pageNum.value = 1
  fetchList()
}

const onCurrentChange = (v) => {
  pageNum.value = v
  fetchList()
}

const onSelectionChange = (selection) => {
  selectedIds.value = (selection || []).map((r) => r?.id).filter((v) => v !== null && v !== undefined)
}

const dialogOpen = ref(false)
const dialogMode = ref('add')
const dialogLoading = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  content: '',
  category: '',
  tag: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.content = ''
  form.category = ''
  form.tag = ''
}

const openAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  dialogOpen.value = true
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  form.id = row?.id ?? null
  form.title = row?.title || ''
  form.content = row?.content || ''
  form.category = row?.category || ''
  form.tag = row?.tag || ''
  dialogOpen.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  dialogLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      const res = await http.post('/admin/knowledge', {
        title: form.title,
        content: form.content,
        category: form.category,
        tag: form.tag
      })
      const result = res?.data
      const ok = result?.code === 1 || result?.code === 200 || result?.success === true
      if (!ok) throw new Error(result?.msg || result?.message || '新增失败')
      ElMessage.success('新增成功')
    } else {
      const res = await http.put('/admin/knowledge', {
        id: form.id,
        title: form.title,
        content: form.content,
        category: form.category,
        tag: form.tag
      })
      const result = res?.data
      const ok = result?.code === 1 || result?.code === 200 || result?.success === true
      if (!ok) throw new Error(result?.msg || result?.message || '更新失败')
      ElMessage.success('更新成功')
    }
    dialogOpen.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    dialogLoading.value = false
  }
}

const removeRow = async (row) => {
  const id = row?.id
  if (!id) return

  try {
    await ElMessageBox.confirm('确定删除该知识条目？此操作不可恢复。', '删除知识', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.delete(`/admin/knowledge/${id}`)
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || '删除失败')
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const batchRemove = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选要删除的条目')
    return
  }

  try {
    await ElMessageBox.confirm(`确定批量删除 ${selectedIds.value.length} 条知识？此操作不可恢复。`, '批量删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.post('/admin/knowledge/batch-delete', selectedIds.value)
    const result = res?.data
    const ok = result?.code === 1 || result?.code === 200 || result?.success === true
    if (!ok) throw new Error(result?.msg || result?.message || '批量删除失败')
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '批量删除失败')
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">通用知识库</div>
      <div class="page-sub">支持分类/标签/关键词筛选；支持新增、编辑、删除、批量删除</div>
    </div>

    <el-card class="card" shadow="never">
      <div class="toolbar">
        <div class="filters">
          <span class="label">分类</span>
          <el-input v-model="query.category" placeholder="例如 交通/票务/路线" style="width: 160px" clearable />

          <span class="label">标签</span>
          <el-input v-model="query.tag" placeholder="例如 佛教文化/演艺" style="width: 160px" clearable />

          <span class="label">关键词</span>
          <el-input v-model="query.keyword" placeholder="标题/内容关键词" style="width: 220px" clearable />

          <el-button type="primary" class="btn" @click="onSearch">查询</el-button>
        </div>

        <div class="actions">
          <el-button type="primary" class="btn-add" @click="openAdd">新增知识</el-button>
          <el-button type="danger" plain @click="batchRemove">批量删除</el-button>
        </div>
      </div>

      <el-table
        :data="rows"
        v-loading="tableLoading"
        class="table"
        stripe
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="52" />
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <div class="title-cell" :title="row?.title || ''">{{ row?.title || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="140">
          <template #default="{ row }">{{ row?.category || '-' }}</template>
        </el-table-column>
        <el-table-column label="标签" width="140">
          <template #default="{ row }">{{ row?.tag || '-' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatTime(row?.updateTime || row?.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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
          :page-sizes="[5, 10, 15]"
          :total="total"
          @size-change="onSizeChange"
          @current-change="onCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogOpen" :title="dialogMode === 'add' ? '新增知识' : '编辑知识'" width="860px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
        <div class="grid">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="form.category" placeholder="例如 交通/票务/路线" />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="form.tag" placeholder="例如 佛教文化/演艺" />
          </el-form-item>
        </div>

        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入知识内容" />
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

.form {
  padding-right: 2px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 14px;
}

.title-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .actions {
    justify-content: flex-end;
  }
}
</style>

