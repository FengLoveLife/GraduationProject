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
        <el-form-item label="进货单号">
          <el-input v-model="filterForm.orderNo" placeholder="请输入进货单号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="filterForm.supplier" placeholder="请输入供应商名称" clearable style="width: 150px" />
        </el-form-item>
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
            <el-option label="待入库" value="pending" />
            <el-option label="已入库" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 进货记录表格 -->
    <el-card shadow="hover" class="records-card">
      <template #header>
        <div class="card-header">
          <span>进货记录列表</span>
          <el-button type="primary" link :icon="Download">导出数据</el-button>
        </div>
      </template>

      <el-table :data="records" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="进货单号" width="150">
          <template #default="{ row }">
            <span class="order-no" @click="showDetail(row)">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="supplier" label="供应商" width="150" />
        <el-table-column prop="productCount" label="商品种类" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.productCount }} 种</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQuantity" label="总数量" width="100" align="center">
          <template #default="{ row }">
            {{ row.totalQuantity }} 件
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120" align="center">
          <template #default="{ row }">
            <span class="amount">¥{{ formatMoney(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column prop="expectedDate" label="预计到货" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'pending'" type="success" link size="small" @click="handleConfirm(row)">
              确认入库
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @change="fetchRecords"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="'进货单详情 - ' + currentRecord?.orderNo" width="800px" destroy-on-close>
      <div class="detail-content" v-if="currentRecord">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="进货单号">{{ currentRecord.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="供应商">{{ currentRecord.supplier }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentRecord.createTime }}</el-descriptions-item>
            <el-descriptions-item label="预计到货">{{ currentRecord.expectedDate }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ currentRecord.creator }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 商品明细 -->
        <div class="detail-section">
          <div class="section-title">商品明细</div>
          <el-table :data="currentRecord.items" stripe size="small">
            <el-table-column prop="productName" label="商品名称" />
            <el-table-column prop="categoryName" label="分类" width="100" />
            <el-table-column prop="quantity" label="数量" width="80" align="center" />
            <el-table-column prop="purchasePrice" label="进货单价" width="100" align="center">
              <template #default="{ row }">¥{{ row.purchasePrice.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="100" align="center">
              <template #default="{ row }">¥{{ (row.quantity * row.purchasePrice).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 金额汇总 -->
        <div class="detail-section">
          <div class="amount-summary">
            <div class="summary-row">
              <span>商品种类：</span>
              <span>{{ currentRecord.productCount }} 种</span>
            </div>
            <div class="summary-row">
              <span>商品总数：</span>
              <span>{{ currentRecord.totalQuantity }} 件</span>
            </div>
            <div class="summary-row highlight">
              <span>进货总额：</span>
              <span>¥{{ formatMoney(currentRecord.totalAmount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'

// 筛选表单
const filterForm = reactive({
  orderNo: '',
  supplier: '',
  dateRange: [],
  status: ''
})

// 进货记录
const records = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 详情
const detailVisible = ref(false)
const currentRecord = ref(null)

// 模拟数据
const mockRecords = [
  {
    id: 1, orderNo: 'PO202603200001', supplier: '康师傅供应商', productCount: 5, totalQuantity: 156, totalAmount: 4850.00,
    createTime: '2026-03-20 10:30:00', expectedDate: '2026-03-22', status: 'pending', creator: '张店长',
    items: [
      { productName: '康师傅红烧牛肉面', categoryName: '方便食品', quantity: 50, purchasePrice: 3.20 },
      { productName: '康师傅老坛酸菜面', categoryName: '方便食品', quantity: 40, purchasePrice: 3.20 },
      { productName: '康师傅香辣牛肉面', categoryName: '方便食品', quantity: 30, purchasePrice: 3.20 },
      { productName: '康师傅桶装红烧面', categoryName: '方便食品', quantity: 20, purchasePrice: 4.50 },
      { productName: '康师傅桶装酸菜面', categoryName: '方便食品', quantity: 16, purchasePrice: 4.50 }
    ]
  },
  {
    id: 2, orderNo: 'PO202603180002', supplier: '可口可乐经销商', productCount: 3, totalQuantity: 120, totalAmount: 1800.00,
    createTime: '2026-03-18 14:20:00', expectedDate: '2026-03-20', status: 'completed', creator: '张店长',
    items: [
      { productName: '可口可乐500ml', categoryName: '饮料', quantity: 50, purchasePrice: 2.50 },
      { productName: '雪碧500ml', categoryName: '饮料', quantity: 40, purchasePrice: 2.50 },
      { productName: '芬达500ml', categoryName: '饮料', quantity: 30, purchasePrice: 2.50 }
    ]
  },
  {
    id: 3, orderNo: 'PO202603150003', supplier: '旺旺食品代理', productCount: 4, totalQuantity: 85, totalAmount: 2450.00,
    createTime: '2026-03-15 09:15:00', expectedDate: '2026-03-17', status: 'completed', creator: '张店长',
    items: [
      { productName: '旺旺雪饼', categoryName: '休闲零食', quantity: 30, purchasePrice: 8.50 },
      { productName: '旺旺仙贝', categoryName: '休闲零食', quantity: 25, purchasePrice: 7.00 },
      { productName: '旺旺牛奶糖', categoryName: '糖果', quantity: 15, purchasePrice: 15.00 },
      { productName: '旺旺浪味仙', categoryName: '休闲零食', quantity: 15, purchasePrice: 12.00 }
    ]
  },
  {
    id: 4, orderNo: 'PO202603100004', supplier: '蒙牛乳业', productCount: 2, totalQuantity: 60, totalAmount: 2100.00,
    createTime: '2026-03-10 16:45:00', expectedDate: '2026-03-12', status: 'cancelled', creator: '张店长',
    items: [
      { productName: '蒙牛纯牛奶250ml', categoryName: '乳制品', quantity: 40, purchasePrice: 3.00 },
      { productName: '蒙牛酸奶', categoryName: '乳制品', quantity: 20, purchasePrice: 4.50 }
    ]
  }
]

// 格式化金额
const formatMoney = (value) => {
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 获取状态类型
const getStatusType = (status) => {
  const types = { pending: 'warning', completed: 'success', cancelled: 'info' }
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const texts = { pending: '待入库', completed: '已入库', cancelled: '已取消' }
  return texts[status] || status
}

// 获取记录
const fetchRecords = async () => {
  // 模拟数据
  records.value = mockRecords
  pagination.total = mockRecords.length
}

// 搜索
const handleSearch = () => {
  fetchRecords()
}

// 重置
const handleReset = () => {
  filterForm.orderNo = ''
  filterForm.supplier = ''
  filterForm.dateRange = []
  filterForm.status = ''
  fetchRecords()
}

// 显示详情
const showDetail = (row) => {
  currentRecord.value = row
  detailVisible.value = true
}

// 确认入库
const handleConfirm = (row) => {
  ElMessage.success(`进货单 ${row.orderNo} 已确认入库`)
  row.status = 'completed'
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

  .amount {
    font-weight: 600;
    color: #F59E0B;
  }
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
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
      span:last-child {
        font-size: 16px;
        font-weight: 600;
        color: #1e293b;
      }
    }

    .highlight span:last-child {
      font-size: 20px;
      color: #EF4444;
    }
  }
}
</style>