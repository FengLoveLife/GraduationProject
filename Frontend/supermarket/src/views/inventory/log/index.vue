<template>
  <div class="inventory-log-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2 class="title">库存变动流水</h2>
        <p class="subtitle">追踪每一笔库存变动，确保账实相符</p>
      </div>
    </div>

    <!-- 顶部搜索区 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="商品信息">
          <el-input
            v-model="queryParams.keyword"
            placeholder="输入商品名称或编码"
            clearable
            @keyup.enter="handleQuery"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="变动类型">
          <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 160px">
            <el-option label="进货入库" :value="1" />
            <el-option label="销售出库" :value="2" />
            <el-option label="损耗盘亏" :value="3" />
            <el-option label="手工调整" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" class="btn-yellow" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Edit" @click="openAdjustDialog">手工盘点调整</el-button>
      <el-button 
        type="default" 
        :icon="Download" 
        :loading="exportLoading" 
        @click="handleExport"
      >
        导出当前流水
      </el-button>
    </div>

    <!-- 数据表格区 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        class="log-table"
      >
        <el-table-column label="流水单号" prop="logNo" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="log-no-text">{{ row.logNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="变动时间" prop="createTime" width="170" align="center" />

        <el-table-column label="商品信息" min-width="220">
          <template #default="{ row }">
            <div class="product-info-cell">
              <el-image
                class="product-img"
                :src="row.imageUrl"
                fit="cover"
              >
                <template #error>
                  <div class="image-slot">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="product-detail">
                <div class="p-name">{{ row.productName }}</div>
                <div class="p-code">{{ row.productCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="变动类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" effect="light">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="变动数量" width="120" align="center">
          <template #default="{ row }">
            <span :class="['change-amount', row.changeAmount > 0 ? 'is-plus' : 'is-minus']">
              {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="库存变化" width="180" align="center">
          <template #default="{ row }">
            <div class="stock-change-cell">
              <span class="stock-before">{{ row.beforeStock }}</span>
              <span class="stock-arrow">➔</span>
              <span class="stock-after">{{ row.afterStock }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作人" prop="operator" width="120" align="center" />
        
        <el-table-column label="备注" prop="remark" min-width="150" show-overflow-tooltip />
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 手工调整弹窗 -->
    <el-dialog
      v-model="adjustVisible"
      title="手工盘点调整"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
      class="adjust-dialog"
    >
      <el-form
        ref="adjustFormRef"
        :model="adjustForm"
        :rules="adjustRules"
        label-width="100px"
      >
        <el-form-item label="调整商品" prop="productId">
          <el-select
            v-model="adjustForm.productId"
            filterable
            remote
            reserve-keyword
            placeholder="输入商品名称或编码搜索"
            :remote-method="remoteSearchProduct"
            :loading="productSearchLoading"
            @change="handleProductChange"
            style="width: 100%"
          >
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="`${item.name} (${item.productCode})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="当前账面库存">
          <span class="current-stock-text">
            {{ currentStock !== null ? currentStock : '--' }}
          </span>
        </el-form-item>

        <el-form-item label="调整后库存">
          <span class="adjusted-stock-text" :class="adjustedStockPreview !== null && adjustedStockPreview < 0 ? 'stock-negative' : 'stock-positive'">
            {{ adjustedStockPreview !== null ? adjustedStockPreview : '--' }}
          </span>
        </el-form-item>

        <el-form-item label="变动类型" prop="type">
          <el-radio-group v-model="adjustForm.type">
            <el-radio :label="3">损耗盘亏</el-radio>
            <el-radio :label="4">手工调整</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="变动数量" prop="changeAmount">
          <el-input-number
            v-model="adjustForm.changeAmount"
            :max="adjustForm.type === 3 ? -1 : undefined"
            style="width: 100%"
          />
          <div class="input-tip" v-if="adjustForm.type === 3">提示：盘亏数量必须为负数</div>
        </el-form-item>

        <el-form-item label="备注说明" prop="remark">
          <el-input
            v-model="adjustForm.remark"
            type="textarea"
            rows="3"
            placeholder="请务必填写调整原因以备审计"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adjustVisible = false">取 消</el-button>
          <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh, Picture, Edit, Download } from '@element-plus/icons-vue'
import request from '../../../utils/request'
import { ElMessage } from 'element-plus'

// --- 状态定义 ---
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 手工调整弹窗相关
const adjustVisible = ref(false)
const adjustLoading = ref(false)
const exportLoading = ref(false)
const adjustFormRef = ref(null)
const productSearchLoading = ref(false)
const productOptions = ref([])
const currentStock = ref(null)

const adjustForm = reactive({
  productId: null,
  type: 4, // 默认手工调整
  changeAmount: 0,
  remark: ''
})

const adjustRules = {
  productId: [{ required: true, message: '请选择调整商品', trigger: 'change' }],
  type: [{ required: true, message: '请选择变动类型', trigger: 'change' }],
  changeAmount: [
    { required: true, message: '请输入变动数量', trigger: 'blur' },
    { validator: (rule, value, callback) => {
        if (value === 0) {
          callback(new Error('变动数量不能为0'))
        } else if (adjustForm.type === 3 && value > 0) {
          callback(new Error('损耗盘亏数量必须为负数'))
        } else {
          callback()
        }
      }, trigger: 'blur' 
    }
  ],
  remark: [{ required: true, message: '请填写调整备注', trigger: 'blur' }]
}

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  type: null,
  dateRange: []
})

// --- 方法定义 ---

// 获取流水列表
const getList = async () => {
  loading.value = true
  try {
    // 构造请求参数
    const params = {
      page: queryParams.page,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword,
      type: queryParams.type
    }
    
    // 处理日期范围
    if (queryParams.dateRange && queryParams.dateRange.length === 2) {
      params.startDate = queryParams.dateRange[0]
      params.endDate = queryParams.dateRange[1]
    }

    const res = await request.get('/inventory-log/page', { params })
    const data = res.data
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取库存流水失败:', error)
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  queryParams.page = 1
  getList()
}

// 重置
const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.type = null
  queryParams.dateRange = []
  queryParams.page = 1
  getList()
}

// 分页大小变化
const handleSizeChange = (val) => {
  queryParams.pageSize = val
  getList()
}

// 页码变化
const handleCurrentChange = (val) => {
  queryParams.page = val
  getList()
}

// 辅助：获取类型文本
const getTypeText = (type) => {
  const map = {
    1: '进货入库',
    2: '销售出库',
    3: '损耗盘亏',
    4: '手工调整'
  }
  return map[type] || '未知'
}

// 辅助：获取类型 Tag 样式
const getTypeTag = (type) => {
  const map = {
    1: 'success',
    2: 'primary',
    3: 'danger',
    4: 'warning'
  }
  return map[type] || 'info'
}

// --- 手工调整逻辑 ---

// 打开弹窗
const openAdjustDialog = () => {
  adjustVisible.value = true
  currentStock.value = null
  productOptions.value = []
  Object.assign(adjustForm, {
    productId: null,
    type: 4,
    changeAmount: 0,
    remark: ''
  })
}

// 远程搜索商品
const remoteSearchProduct = async (query) => {
  if (query) {
    productSearchLoading.value = true
    try {
      const res = await request.get('/product/page', {
        params: { keyword: query, pageSize: 50 }
      })
      productOptions.value = res.data.records || []
    } finally {
      productSearchLoading.value = false
    }
  } else {
    productOptions.value = []
  }
}

// 调整后库存预览
const adjustedStockPreview = computed(() => {
  if (currentStock.value === null || !adjustForm.changeAmount) return null
  return currentStock.value + (adjustForm.changeAmount || 0)
})

// 选中商品联动
const handleProductChange = (val) => {
  const product = productOptions.value.find(p => p.id === val)
  currentStock.value = product ? product.stock : null
}

// 提交调整
const submitAdjust = async () => {
  if (!adjustFormRef.value) return
  await adjustFormRef.value.validate(async (valid) => {
    if (valid) {
      adjustLoading.value = true
      try {
        await request.post('/inventory-log/adjust', adjustForm)
        ElMessage.success('库存调整成功')
        adjustVisible.value = false
        getList() // 刷新列表
      } finally {
        adjustLoading.value = false
      }
    }
  })
}

// 导出 Excel
const handleExport = async () => {
  exportLoading.value = true
  try {
    // 1. 组装查询参数 (不含分页)
    const params = {
      keyword: queryParams.keyword,
      type: queryParams.type
    }
    if (queryParams.dateRange && queryParams.dateRange.length === 2) {
      params.startDate = queryParams.dateRange[0]
      params.endDate = queryParams.dateRange[1]
    }

    // 2. 发起导出请求
    const res = await request.get('/inventory-log/export', {
      params,
      responseType: 'blob' // 核心：处理二进制流
    })

    // 3. 处理文件下载 (兼容拦截器直接返回 data 或 res 对象的情况)
    const blobData = res.data || res
    const blob = new Blob([blobData], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    // 4. 提取文件名
    let fileName = `库存流水账单_${new Date().getTime()}.xlsx`
    const contentDisposition = res.headers ? res.headers['content-disposition'] : ''
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename\*=utf-8''(.+)$/) || contentDisposition.match(/filename=(.+)$/)
      if (fileNameMatch && fileNameMatch[1]) {
        fileName = decodeURIComponent(fileNameMatch[1])
      }
    }

    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()

    // 5. 资源释放
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.inventory-log-container {
  padding: 20px;
  background-color: #f8fafc;
  min-height: calc(100vh - 100px);
}

.page-header {
  background: linear-gradient(135deg, #e67e00 0%, #ff9800 45%, #ffce00 100%);
  border-radius: 12px;
  padding: 24px 28px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(230, 126, 0, 0.28);
  color: #fff;
}

.page-header .title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px 0;
  color: #fff;
}

.page-header .subtitle {
  font-size: 13px;
  margin: 0;
  color: rgba(255, 255, 255, 0.85);
}

.adjusted-stock-text {
  font-size: 16px;
  font-weight: 600;
}

.stock-positive {
  color: #67C23A;
}

.stock-negative {
  color: #F56C6C;
}

.search-card {
  margin-bottom: 16px;
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.table-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.action-bar {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-yellow {
  background-color: #ffce00 !important;
  border-color: #ffce00 !important;
  color: #333 !important;
  font-weight: bold;
}

.btn-yellow:hover {
  background-color: #ffd700 !important;
  border-color: #ffd700 !important;
  opacity: 0.9;
}

.log-no-text {
  color: #909399;
  font-size: 13px;
  font-family: monospace;
}

.product-info-cell {
  display: flex;
  align-items: center;
}

.product-img {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  flex-shrink: 0;
  border: 1px solid #f1f5f9;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 20px;
}

.product-detail {
  margin-left: 12px;
  overflow: hidden;
}

.p-name {
  font-weight: bold;
  color: #334155;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.p-code {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.change-amount {
  font-weight: 800;
  font-size: 15px;
}

.is-plus {
  color: #67c23a;
}

.is-minus {
  color: #f56c6c;
}

.stock-change-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.stock-before {
  color: #94a3b8;
  font-size: 13px;
}

.stock-arrow {
  color: #cbd5e1;
  font-weight: bold;
}

.stock-after {
  color: #1e293b;
  font-weight: bold;
  font-size: 14px;
}

.current-stock-text {
  font-size: 18px;
  font-weight: bold;
  color: #1e293b;
}

.input-tip {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
}

.adjust-dialog {
  :deep(.el-dialog__body) {
    padding-top: 10px;
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table__header th) {
  background-color: #f8fafc !important;
  color: #475569;
  font-weight: 800;
}
</style>
