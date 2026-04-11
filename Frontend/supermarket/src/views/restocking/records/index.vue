<template>
  <div class="restocking-records">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">进货记录</h2>
        <p class="subtitle">查看历史进货单记录和入库情况</p>
      </div>
    </div>

    <!-- 筛选条件 -->
    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="待确认" :value="0" />
            <el-option label="已下单" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 进货记录表格 -->
    <el-card shadow="hover" class="records-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>进货记录列表</span>
          <el-tag type="info" size="small">共 {{ records.length }} 条记录</el-tag>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-if="records.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无进货记录">
          <template #description>
            <p>还没有进货单记录</p>
            <p class="empty-tip">请先在"进货建议"页面生成进货单</p>
          </template>
        </el-empty>
      </div>

      <!-- 表格数据 -->
      <el-table v-else :data="records" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="进货单号" width="160">
          <template #default="{ row }">
            <span class="order-no" @click="showDetail(row)">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalQuantity" label="总数量" width="100" align="center">
          <template #default="{ row }">
            {{ row.totalQuantity }} 件
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="130" align="center">
          <template #default="{ row }">
            <span class="amount">¥{{ formatMoney(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" label="下单日期" width="120" align="center" />
        <el-table-column prop="expectedDate" label="预计到货" width="140" align="center">
          <template #default="{ row }">
            <span v-if="!row.expectedDate">-</span>
            <span v-else-if="isOverdue(row)" class="overdue-date">
              {{ row.expectedDate }} <el-tag type="danger" size="small" effect="dark">逾期</el-tag>
            </span>
            <span v-else>{{ row.expectedDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" type="warning" link size="small" @click="handlePlace(row)">
              已下单
            </el-button>
            <el-button v-if="row.status === 1" type="success" link size="small" @click="handleConfirm(row)">
              确认入库
            </el-button>
            <el-button v-if="row.status === 0 || row.status === 1" type="danger" link size="small" @click="handleCancel(row)">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="'进货单详情 - ' + currentRecord?.orderNo" width="800px" destroy-on-close>
      <div class="detail-content" v-if="currentRecord" v-loading="detailLoading">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="进货单号">{{ currentRecord.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentRecord.status)">{{ currentRecord.statusText }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="操作人">{{ currentRecord.operator }}</el-descriptions-item>
            <el-descriptions-item label="下单日期">{{ currentRecord.orderDate }}</el-descriptions-item>
            <el-descriptions-item label="预计到货">{{ currentRecord.expectedDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="实际到货">{{ currentRecord.actualArrivalDate || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 商品明细 -->
        <div class="detail-section">
          <div class="section-title">商品明细</div>
          <el-table :data="currentRecord.items" stripe size="small">
            <el-table-column prop="productCode" label="商品编码" width="100" />
            <el-table-column prop="productName" label="商品名称" />
            <el-table-column prop="categoryName" label="分类" width="100" />
            <el-table-column prop="quantity" label="数量" width="80" align="center" />
            <el-table-column prop="purchasePrice" label="进货单价" width="100" align="center">
              <template #default="{ row }">¥{{ (row.purchasePrice || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="100" align="center">
              <template #default="{ row }">¥{{ ((row.quantity || 0) * (row.purchasePrice || 0)).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 金额汇总 -->
        <div class="detail-section">
          <div class="amount-summary">
            <div class="summary-row">
              <span>商品种类</span>
              <span class="value">{{ currentRecord.items?.length || 0 }} 种</span>
            </div>
            <div class="summary-row">
              <span>商品总数</span>
              <span class="value">{{ currentRecord.totalQuantity }} 件</span>
            </div>
            <div class="summary-row highlight">
              <span>进货总额</span>
              <span class="value">¥{{ formatMoney(currentRecord.totalAmount) }}</span>
            </div>
          </div>
        </div>

        <!-- 备注 -->
        <div class="detail-section" v-if="currentRecord.remark">
          <div class="section-title">备注</div>
          <div class="remark-content">{{ currentRecord.remark }}</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentRecord?.status === 0" type="warning" @click="handlePlaceFromDetail">
          已下单
        </el-button>
        <el-button v-if="currentRecord?.status === 1" type="success" @click="handleConfirmFromDetail">
          确认入库
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getPurchaseOrderList, getPurchaseOrderDetail, placeOrder, confirmOrderArrival, cancelPurchaseOrder } from '@/api/restocking'

// 加载状态
const loading = ref(false)
const detailLoading = ref(false)

// 筛选表单
const filterForm = reactive({
  dateRange: [],
  status: null
})

// 进货记录
const records = ref([])

// 详情
const detailVisible = ref(false)
const currentRecord = ref(null)

// 格式化金额
const formatMoney = (value) => {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 获取状态类型
const getStatusType = (status) => {
  const types = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }
  return types[status] || 'info'
}

// 获取记录
const fetchRecords = async () => {
  loading.value = true

  try {
    const params = {
      status: filterForm.status,
      startDate: filterForm.dateRange?.[0],
      endDate: filterForm.dateRange?.[1]
    }

    const res = await getPurchaseOrderList(params)
    if (res.code === 200 && res.data) {
      records.value = res.data
    }
  } catch (error) {
    console.error('获取进货记录失败:', error)
    ElMessage.error('获取进货记录失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  fetchRecords()
}

// 重置
const handleReset = () => {
  filterForm.dateRange = []
  filterForm.status = null
  fetchRecords()
}

// 显示详情
const showDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true

  try {
    const res = await getPurchaseOrderDetail(row.id)
    if (res.code === 200 && res.data) {
      currentRecord.value = res.data
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 判断是否逾期（已下单状态 + 预计到货日已过）
const isOverdue = (row) => {
  return row.status === 1 && row.expectedDate && row.expectedDate < new Date().toISOString().slice(0, 10)
}

// 标记已下单
const handlePlace = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认已向供应商下单 ${row.orderNo}，货物正在途中？`,
      '标记已下单',
      { type: 'info' }
    )
    const res = await placeOrder(row.id)
    if (res.code === 200) {
      ElMessage.success('已标记为已下单')
      await fetchRecords()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

// 从详情弹窗标记已下单
const handlePlaceFromDetail = async () => {
  if (!currentRecord.value) return
  try {
    await ElMessageBox.confirm('确认已向供应商下单，货物正在途中？', '标记已下单', { type: 'info' })
    const res = await placeOrder(currentRecord.value.id)
    if (res.code === 200) {
      ElMessage.success('已标记为已下单')
      detailVisible.value = false
      await fetchRecords()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

// 确认入库
const handleConfirm = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认进货单 ${row.orderNo} 已到货入库？\n入库后将自动更新库存。`,
      '确认入库',
      { type: 'warning' }
    )

    const res = await confirmOrderArrival(row.id)
    if (res.code === 200) {
      ElMessage.success('入库确认成功，库存已更新')
      await fetchRecords()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('入库确认失败:', error)
      ElMessage.error('入库确认失败')
    }
  }
}

// 从详情弹窗确认入库
const handleConfirmFromDetail = async () => {
  if (!currentRecord.value) return

  try {
    await ElMessageBox.confirm(
      '确认入库后将自动更新库存，是否继续？',
      '确认入库',
      { type: 'warning' }
    )

    const res = await confirmOrderArrival(currentRecord.value.id)
    if (res.code === 200) {
      ElMessage.success('入库确认成功，库存已更新')
      detailVisible.value = false
      await fetchRecords()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('入库确认失败:', error)
      ElMessage.error('入库确认失败')
    }
  }
}

// 取消进货单
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消进货单 ${row.orderNo} 吗？`,
      '取消进货单',
      { type: 'warning' }
    )

    const res = await cancelPurchaseOrder(row.id)
    if (res.code === 200) {
      ElMessage.success('进货单已取消')
      await fetchRecords()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消失败:', error)
      ElMessage.error('取消失败')
    }
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped lang="scss">
.restocking-records {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 100px);
}

.page-header {
  margin-bottom: 24px;

  .title {
    font-size: 24px;
    font-weight: 800;
    color: #1e293b;
    margin: 0;
  }
  .subtitle {
    font-size: 14px;
    color: #64748b;
    margin: 8px 0 0;
  }
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.records-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);

  :deep(.el-card__header) {
    border-bottom: 1px solid #f1f5f9;
    padding: 16px 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 700;
    color: #334155;
  }

  .order-no {
    color: #409EFF;
    cursor: pointer;
    font-weight: 600;
    &:hover { text-decoration: underline; }
  }

  .overdue-date {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: #EF4444;
    font-weight: 600;
  }

  .amount {
    font-weight: 600;
    color: #F59E0B;
  }

  .empty-state {
    padding: 60px 20px;
    text-align: center;

    .empty-tip {
      font-size: 13px;
      color: #909399;
      margin-top: 8px;
    }
  }
}

.detail-content {
  .detail-section {
    margin-bottom: 24px;

    .section-title {
      font-size: 15px;
      font-weight: 700;
      color: #1e293b;
      margin-bottom: 12px;
      padding-left: 8px;
      border-left: 3px solid #3B82F6;
    }
  }

  .remark-content {
    background: #f8fafc;
    padding: 12px 16px;
    border-radius: 8px;
    color: #64748b;
  }

  .amount-summary {
    background: #f8fafc;
    border-radius: 12px;
    padding: 20px;
    display: flex;
    justify-content: flex-end;
    gap: 40px;

    .summary-row {
      display: flex;
      flex-direction: column;
      align-items: center;

      span:first-child {
        font-size: 12px;
        color: #64748b;
        margin-bottom: 4px;
      }

      .value {
        font-size: 16px;
        font-weight: 600;
        color: #1e293b;
      }
    }

    .highlight .value {
      font-size: 20px;
      color: #EF4444;
    }
  }
}
</style>