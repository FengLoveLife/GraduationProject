<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../../utils/request'
import { Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])

// 新增分类弹窗相关状态
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = ref({
  id: null,
  name: '',
  parentId: 0,
  sortOrder: 0,
  status: 1,
})

const rules = {
  name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
}

const rowClassName = ({ row }) => {
  const lv = Number(row.level || 1)
  return `lv-${Number.isFinite(lv) && lv > 0 ? lv : 1}`
}

async function fetchTree() {
  loading.value = true
  try {
    const res = await request.get('/category/tree')
    tableData.value = Array.isArray(res.data) ? res.data : []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function levelText(level) {
  const n = Number(level)
  if (!Number.isFinite(n) || n <= 0) return '-'
  if (n === 1) return '一级'
  if (n === 2) return '二级'
  if (n === 3) return '三级'
  return `${n}级`
}

function levelType(level) {
  const n = Number(level)
  if (n === 1) return 'warning' // 一级：黄色
  if (n === 2) return 'success' // 二级：绿色
  if (n === 3) return 'info' // 三级：蓝色/信息色
  return '' // 其他层级默认
}

function statusLabel(status) {
  return Number(status) === 1 ? '启用' : '禁用'
}

function statusType(status) {
  return Number(status) === 1 ? 'success' : 'info'
}

function canAddSub(row) {
  const lv = Number(row?.level)
  return !Number.isFinite(lv) || lv < 3
}

function resetForm() {
  if (formRef.value && typeof formRef.value.resetFields === 'function') {
    formRef.value.resetFields()
  }
  formData.value = {
    id: null,
    name: '',
    parentId: 0,
    sortOrder: 0,
    status: 1,
  }
}

// 新增一级分类
function handleAddTop() {
  resetForm()
  formData.value.parentId = 0
  dialogTitle.value = '新增一级分类'
  dialogVisible.value = true
}

// 新增子分类
function handleAddSub(row) {
  if (!row || !row.id) {
    ElMessage.error('父级分类信息异常，无法新增子分类')
    return
  }
  resetForm()
  formData.value.parentId = row.id
  dialogTitle.value = '新增子分类'
  dialogVisible.value = true
}

function handleEdit(row) {
  if (!row || !row.id) {
    ElMessage.error('分类信息异常，无法编辑')
    return
  }
  resetForm()
  formData.value = {
    id: row.id,
    name: row.name,
    parentId: row.parentId ?? 0,
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
  }
  dialogTitle.value = '编辑分类'
  dialogVisible.value = true
}

async function handleDelete(row) {
  if (!row || !row.id) {
    ElMessage.error('分类信息异常，无法删除')
    return
  }

  try {
    // 1) 弹出二次确认框
    await ElMessageBox.confirm(
      `确定要永久删除分类【${row.name}】吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    // 2) 用户点击确定后，发送真实 DELETE 请求
    loading.value = true
    try {
      await request.delete(`/category/${row.id}`)
      ElMessage.success('删除分类成功')
      // 3) 刷新树形列表
      await fetchTree()
    } catch (e) {
      // 这里的错误会被 request.js 拦截器捕获并提示，也可以在这里处理特定逻辑
      const msg = e?.msg || e?.message || '删除分类失败'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  } catch (err) {
    // 4) 用户点击取消
    ElMessage.info('已取消删除')
  }
}

// 提交分类表单（新增或编辑）
function submitForm() {
  if (!formRef.value) return

  formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const isEdit = !!formData.value.id
      const payload = {
        id: formData.value.id,
        name: formData.value.name,
        parentId: formData.value.parentId ?? 0,
        sortOrder: formData.value.sortOrder ?? 0,
        status: formData.value.status ?? 1,
      }

      if (isEdit) {
        await request.put('/category', payload)
        ElMessage.success('修改分类成功')
      } else {
        await request.post('/category', payload)
        ElMessage.success('新增分类成功')
      }

      dialogVisible.value = false
      await fetchTree()
    } catch (e) {
      const isEdit = !!formData.value.id
      const msg = e?.msg || e?.message || (isEdit ? '修改分类失败' : '新增分类失败')
      ElMessage.error(msg)
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => {
  fetchTree()
})
</script>

<template>
  <div class="category-page">
    <el-card class="card" shadow="never" v-loading="loading">
      <template #header>
        <div class="toolbar">
          <div class="title-wrap">
            <div class="title">商品分类</div>
            <div class="desc">管理超市的所有商品类目层级与状态</div>
          </div>

          <div class="actions">
            <el-button class="btn-yellow" type="primary" :icon="Plus" @click="handleAddTop">
              新增一级分类
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        class="table"
        :data="tableData"
        row-key="id"
        :tree-props="{ children: 'children' }"
        :row-class-name="rowClassName"
      >
        <!-- 使用默认单元格，让树形展开图标与名称同一行，内置缩进生效 -->
        <el-table-column prop="name" label="分类名称" min-width="220" show-overflow-tooltip />

        <el-table-column label="层级" width="96" align="center">
          <template #default="{ row }">
            <el-tag class="lvl-tag" :type="levelType(row.level)" size="small" effect="plain">
              {{ levelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <div class="status">
              <span class="dot" :class="{ on: Number(row.status) === 1 }" />
              <el-tag :type="statusType(row.status)" size="small" effect="plain">
                {{ statusLabel(row.status) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="排序权重" width="110" align="center">
          <template #default="{ row }">
            <span class="sort">{{ row.sortOrder ?? '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="360" align="center" fixed="right">
          <template #default="{ row }">
            <div class="op">
              <el-button
                class="op-btn mini-yellow"
                type="primary"
                plain
                size="small"
                :disabled="!canAddSub(row)"
                @click="handleAddSub(row)"
              >
                <el-icon><Plus /></el-icon>
                新增子分类
              </el-button>
              <el-button class="op-btn" type="primary" plain size="small" @click="handleEdit(row)">
                <el-icon><EditPen /></el-icon>
                编辑
              </el-button>
              <el-button class="op-btn danger" type="danger" plain size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增分类弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="90px"
        label-position="right"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入分类名称"
            maxlength="50"
            show-word-limit
            clearable
          />
        </el-form-item>

        <el-form-item label="排序权重">
          <el-input-number
            v-model="formData.sortOrder"
            :min="0"
            :step="1"
            controls-position="right"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-switch
            v-model="formData.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button :disabled="submitLoading" @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">
            确 定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
$yellow: #ffce00;
$dark: #2b2f3a;

.category-page {
  padding: 16px;
}

.card {
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 14px 40px rgba(17, 24, 39, 0.06);

  :deep(.el-card__header) {
    padding: 16px 16px 14px;
    border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  }
  :deep(.el-card__body) {
    padding: 12px 12px 16px;
  }
}

.toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.title-wrap {
  min-width: 0;
}

.title {
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: 0.2px;
}

.desc {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.btn-yellow {
  --el-color-primary: #{$yellow};
  --el-color-primary-light-3: #ffe57a;
  --el-color-primary-dark-2: #f1bf00;
  border-color: #{$yellow};
  background: #{$yellow};
  color: #111;
  font-weight: 900;
  border-radius: 12px;
  height: 38px;
  transition: transform 0.15s ease, filter 0.15s ease;

  &:hover {
    transform: translateY(-1px) scale(1.02);
    filter: brightness(0.98);
  }
  &:active {
    transform: translateY(0) scale(1);
    filter: brightness(0.95);
  }
}

.table {
  border-radius: 12px;
  overflow: hidden;

  :deep(.el-table__header-wrapper th) {
    background: #fbfbfc;
    color: #334155;
    font-weight: 800;
  }

  :deep(.el-table__row:hover td) {
    background: rgba(255, 206, 0, 0.06) !important;
  }
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.name {
  font-weight: 800;
  color: #0f172a;
}

.lvl-tag {
  border-radius: 999px;
  padding: 0 14px;
  font-weight: 800;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #cbd5e1;
  box-shadow: 0 0 0 3px rgba(203, 213, 225, 0.25);
}
.dot.on {
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18);
}

.sort {
  font-weight: 800;
  color: #334155;
}

.muted {
  color: #94a3b8;
}

.op {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  justify-content: center;
  white-space: nowrap;
}

.op-btn {
  font-weight: 800;
  transition: transform 0.15s ease, filter 0.15s ease;

  :deep(.el-icon) {
    margin-right: 6px;
  }

  &:hover {
    transform: translateY(-1px);
    filter: brightness(0.95);
  }
}

.op-btn.danger:hover {
  filter: brightness(0.9);
}

// 表格内“子分类”按钮：统一品牌黄（比默认 primary 更贴合整体）
.mini-yellow {
  --el-color-primary: #{$yellow};
  --el-color-primary-light-3: #ffe57a;
  --el-color-primary-dark-2: #f1bf00;
  color: #7c5b00;
  border-color: rgba(255, 206, 0, 0.55);
  background: rgba(255, 206, 0, 0.14);

  :deep(.el-icon) {
    color: #7c5b00;
  }

  &:hover {
    border-color: rgba(255, 206, 0, 0.8);
    background: rgba(255, 206, 0, 0.2);
  }
}

// 不同层级行的视觉区分（利用 el-table 的行 class）
:deep(.el-table__row.lv-1) .name {
  color: #111827;
}
:deep(.el-table__row.lv-2) .name {
  color: #0f172a;
}
:deep(.el-table__row.lv-3) .name {
  color: #334155;
}

</style>

