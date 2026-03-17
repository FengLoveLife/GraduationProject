<template>
  <div class="sales-list-container">
    <!-- 顶部搜索区 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="订单编号">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="请输入订单编号"
            clearable
            @keyup.enter="handleQuery"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="销售日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-yellow" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格区 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        class="sales-table"
      >
        <el-table-column label="订单编号" prop="orderNo" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="order-no-text">{{ row.orderNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="销售时间" prop="saleTime" width="170" align="center" />

        <el-table-column label="订单总金额" width="140" align="right">
          <template #default="{ row }">
            <span class="amount-text">¥ {{ row.totalAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="商品总件数" prop="totalQuantity" width="120" align="center" />

        <el-table-column label="支付方式" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getPaymentTypeTag(row.paymentType)" effect="light">
              {{ getPaymentTypeText(row.paymentType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="收银机" prop="operator" width="120" align="center" />

        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="handleViewDetails(row)">查看明细</el-button>
          </template>
        </el-table-column>
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

    <!-- 订单明细弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="`订单明细 - ${currentOrderNo}`"
      width="800px"
      destroy-on-close
      class="detail-dialog"
    >
      <el-table
        v-loading="detailLoading"
        :data="detailList"
        border
        stripe
        style="width: 100%"
        max-height="500"
      >
        <el-table-column label="商品编码" prop="productCode" width="140" />
        <el-table-column label="商品名称" prop="productName" min-width="180" show-overflow-tooltip />
        <el-table-column label="实际售价" width="120" align="right">
          <template #default="{ row }">
            <span>¥ {{ row.unitPrice?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销售数量" prop="quantity" width="100" align="center" />
        <el-table-column label="小计金额" width="120" align="right">
          <template #default="{ row }">
            <span class="subtotal-text">¥ {{ row.subtotalAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="小计毛利" width="120" align="right">
          <template #default="{ row }">
            <span class="profit-text">+ ¥ {{ row.subtotalProfit?.toFixed(2) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import request from '../../../utils/request'

// --- 状态定义 ---
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  orderNo: '',
  startDate: '',
  endDate: ''
})

// 明细弹窗相关
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailList = ref([])
const currentOrderNo = ref('')

// --- 方法定义 ---

// 获取列表数据
const getList = async () => {
  loading.value = true
  try {
    // 处理日期范围
    if (dateRange.value && dateRange.value.length === 2) {
      queryParams.startDate = dateRange.value[0]
      queryParams.endDate = dateRange.value[1]
    } else {
      queryParams.startDate = ''
      queryParams.endDate = ''
    }

    const res = await request.get('/sales-order/page', { params: queryParams })
    const data = res.data
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取销售流水失败:', error)
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
  queryParams.orderNo = ''
  dateRange.value = []
  queryParams.page = 1
  getList()
}

// 分页处理
const handleSizeChange = (val) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val) => {
  queryParams.page = val
  getList()
}

// 查看明细
const handleViewDetails = async (row) => {
  currentOrderNo.value = row.orderNo
  detailVisible.value = true
  detailLoading.value = true
  detailList.value = []

  try {
    const res = await request.get(`/sales-order/${row.id}/items`)
    detailList.value = res.data || []
  } catch (error) {
    console.error('获取订单明细失败:', error)
  } finally {
    detailLoading.value = false
  }
}

// 辅助：支付方式文本
const getPaymentTypeText = (type) => {
  const map = {
    1: '现金',
    2: '微信',
    3: '支付宝'
  }
  return map[type] || '未知'
}

// 辅助：支付方式 Tag 样式
const getPaymentTypeTag = (type) => {
  const map = {
    1: 'info',
    2: 'success',
    3: 'primary'
  }
  return map[type] || ''
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.sales-list-container {
  padding: 20px;
  background-color: #f8fafc;
  min-height: calc(100vh - 100px);
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

.order-no-text {
  color: #606266;
  font-family: monospace;
  font-weight: 600;
}

.amount-text {
  color: #303133;
  font-weight: bold;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

.subtotal-text {
  font-weight: bold;
  color: #303133;
}

.profit-text {
  color: #67c23a;
  font-weight: bold;
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

.detail-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
  padding-bottom: 20px;
}
</style>
