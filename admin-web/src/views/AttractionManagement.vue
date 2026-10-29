<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

// 景点库（结构化数据）管理
// 对接后端：/admin/attraction
// - GET    /page        分页查询（keyword, isHot）
// - POST   /            新增（Attraction）
// - PUT    /            编辑（Attraction）
// - DELETE /{attrId}    删除

const query = reactive({
  keyword: '',
  isHot: undefined
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
    const res = await http.get('/admin/attraction/page', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: query.keyword || undefined,
        isHot: query.isHot === undefined ? undefined : query.isHot
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
  attrId: '',
  attrName: '',
  attrType: '',
  location: '',
  highlights: '',
  buildingInfo: '',
  features: '',
  culture: '',
  detail: '',
  performanceInfo: '',
  remark: '',
  isHot: 0
})

const rules = {
  attrId: [{ required: true, message: '请输入景点ID', trigger: 'blur' }],
  attrName: [{ required: true, message: '请输入景点名称', trigger: 'blur' }],
  attrType: [{ required: true, message: '请输入类型', trigger: 'blur' }],
  location: [{ required: true, message: '请输入位置', trigger: 'blur' }]
}

const resetForm = () => {
  form.attrId = ''
  form.attrName = ''
  form.attrType = ''
  form.location = ''
  form.highlights = ''
  form.buildingInfo = ''
  form.features = ''
  form.culture = ''
  form.detail = ''
  form.performanceInfo = ''
  form.remark = ''
  form.isHot = 0
}

const openAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  dialogOpen.value = true
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  form.attrId = row?.attrId || ''
  form.attrName = row?.attrName || ''
  form.attrType = row?.attrType || ''
  form.location = row?.location || ''
  form.highlights = row?.highlights || ''
  form.buildingInfo = row?.buildingInfo || ''
  form.features = row?.features || ''
  form.culture = row?.culture || ''
  form.detail = row?.detail || ''
  form.performanceInfo = row?.performanceInfo || ''
  form.remark = row?.remark || ''
  form.isHot = Number(row?.isHot ?? 0)
  dialogOpen.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  dialogLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      // 不支持批量导入：景点数据重要，必须逐条审核
      const res = await http.post('/admin/attraction', {
        ...form,
        isHot: Number(form.isHot) ? 1 : 0
      })
      const result = res?.data
      const ok = result?.code === 1 || result?.code === 200 || result?.success === true
      if (!ok) throw new Error(result?.msg || result?.message || '新增失败')
      ElMessage.success('新增成功')
    } else {
      const res = await http.put('/admin/attraction', {
        ...form,
        isHot: Number(form.isHot) ? 1 : 0
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
  const attrId = row?.attrId
  if (!attrId) return

  try {
    await ElMessageBox.confirm('确定删除该景点？此操作不可恢复。', '删除景点', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await http.delete(`/admin/attraction/${encodeURIComponent(attrId)}`)
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
      <div class="page-title">景点库</div>
      <div class="page-sub">表格展示16个景点：名称、类型、位置、是否热门；支持逐条新增/编辑/删除</div>
    </div>

    <el-card class="card" shadow="never">
      <div class="toolbar">
        <div class="filters">
          <span class="label">关键词</span>
          <el-input v-model="query.keyword" placeholder="名称/类型/位置关键词" style="width: 240px" clearable />

          <span class="label">热门</span>
          <el-select v-model="query.isHot" placeholder="全部" style="width: 140px" clearable>
            <el-option label="全部" :value="undefined" />
            <el-option label="热门" :value="1" />
            <el-option label="非热门" :value="0" />
          </el-select>

          <el-button type="primary" class="btn" @click="onSearch">查询</el-button>
        </div>

        <el-button type="primary" class="btn-add" @click="openAdd">新增景点</el-button>
      </div>

      <el-table :data="rows" v-loading="tableLoading" class="table" stripe>
        <el-table-column label="景点名称" min-width="180">
          <template #default="{ row }">{{ row?.attrName || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" min-width="140">
          <template #default="{ row }">{{ row?.attrType || '-' }}</template>
        </el-table-column>
        <el-table-column label="位置" min-width="200">
          <template #default="{ row }">{{ row?.location || '-' }}</template>
        </el-table-column>
        <el-table-column label="是否热门" width="120">
          <template #default="{ row }">
            <el-tag v-if="String(row?.isHot) === '1'" type="warning" effect="dark">热门</el-tag>
            <el-tag v-else type="info" effect="plain">普通</el-tag>
          </template>
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

    <el-dialog v-model="dialogOpen" :title="dialogMode === 'add' ? '新增景点' : '编辑景点'" width="860px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
        <div class="grid">
          <el-form-item label="景点ID" prop="attrId">
            <el-input v-model="form.attrId" :disabled="dialogMode === 'edit'" placeholder="例如 A01 / LS-001" />
          </el-form-item>
          <el-form-item label="景点名称" prop="attrName">
            <el-input v-model="form.attrName" placeholder="请输入景点名称" />
          </el-form-item>
          <el-form-item label="类型" prop="attrType">
            <el-input v-model="form.attrType" placeholder="例如 佛教建筑/演艺/自然景观" />
          </el-form-item>
          <el-form-item label="位置" prop="location">
            <el-input v-model="form.location" placeholder="请输入位置描述" />
          </el-form-item>
          <el-form-item label="是否热门">
            <el-switch v-model="form.isHot" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>

        <el-form-item label="亮点（highlights)">
          <el-input v-model="form.highlights" type="textarea" :rows="2" placeholder="亮点简介" />
        </el-form-item>
        <el-form-item label="建筑信息（buildingInfo)">
          <el-input v-model="form.buildingInfo" type="textarea" :rows="2" placeholder="建筑参数/规模/材质等" />
        </el-form-item>
        <el-form-item label="特色（features)">
          <el-input v-model="form.features" type="textarea" :rows="2" placeholder="特色说明" />
        </el-form-item>
        <el-form-item label="文化内涵（culture)">
          <el-input v-model="form.culture" type="textarea" :rows="2" placeholder="佛教文化/历史故事等" />
        </el-form-item>
        <el-form-item label="详细介绍（detail)">
          <el-input v-model="form.detail" type="textarea" :rows="3" placeholder="更完整的介绍" />
        </el-form-item>
        <el-form-item label="演出信息（performanceInfo)">
          <el-input v-model="form.performanceInfo" type="textarea" :rows="2" placeholder="演艺时间/场次等" />
        </el-form-item>
        <el-form-item label="备注（remark)">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="管理员备注" />
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

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

