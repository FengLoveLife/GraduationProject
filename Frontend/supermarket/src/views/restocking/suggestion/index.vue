<template>
  <div class="restocking-suggestion">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">智能进货建议</h2>
        <p class="subtitle">基于销量预测结果，智能计算最优进货量</p>
      </div>
      <div class="header-right">
        <el-button type="success" :icon="Document" @click="handleGenerateOrder" :disabled="suggestions.length === 0">
          生成进货单
        </el-button>
      </div>
    </div>

    <!-- 汇总统计 -->
    <el-row :gutter="20" class="summary-row">
      <el-col :span="6">
        <div class="summary-card summary-red">
          <div class="summary-icon"><Warning /></div>
          <div class="summary-content">
            <div class="summary-value">{{ redCount }}</div>
            <div class="summary-label">红灯（必须补货）</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card summary-orange">
          <div class="summary-icon"><AlarmClock /></div>
          <div class="summary-content">
            <div class="summary-value">{{ yellowCount }}</div>
            <div class="summary-label">黄灯（顺带补货）</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card summary-green">
          <div class="summary-icon"><Box /></div>
          <div class="summary-content">
            <div class="summary-value">{{ totalQuantity }}</div>
            <div class="summary-label">总进货数量</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card summary-purple">
          <div class="summary-icon"><Money /></div>
          <div class="summary-content">
            <div class="summary-value">¥{{ formatMoney(totalAmount) }}</div>
            <div class="summary-label">预计进货金额</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 算法说明 -->
    <el-alert type="info" :closable="false" show-icon class="algorithm-alert">
      <template #title>
        <span>智能进货策略：<strong>红黄绿灯·动态凑单模型</strong></span>
      </template>
      <template #default>
        <div class="algorithm-detail">
          <span class="light-item"><el-tag type="danger" size="small">红灯</el-tag> 当前库存 ≤ 安全库存，必须补货</span>
          <span class="light-item"><el-tag type="warning" size="small">黄灯</el-tag> 库存撑不过周期30%，顺带补货</span>
          <span class="light-item">目标库存 = 日均销量 × 补货周期 + 安全库存</span>
        </div>
      </template>
    </el-alert>

    <!-- 进货建议表格 -->
    <el-card shadow="hover" class="suggestion-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>进货建议清单</span>
            <el-tag type="warning" effect="plain" size="small">共 {{ suggestions.length }} 项</el-tag>
            <el-radio-group v-model="lightFilter" size="small" @change="handleLightFilterChange" class="light-filter">
              <el-radio-button :value="0">全部</el-radio-button>
              <el-radio-button :value="1">仅红灯</el-radio-button>
              <el-radio-button :value="2">仅黄灯</el-radio-button>
            </el-radio-group>
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Refresh" @click="handleRefresh" :loading="loading">生成进货建议</el-button>
            <el-button :icon="Download" @click="handleExport" :disabled="suggestions.length === 0">导出清单</el-button>
          </div>
        </div>
      </template>

      <!-- 空状态提示 -->
      <div v-if="suggestions.length === 0" class="empty-state">
        <el-empty description="暂无进货建议">
          <template #image>
            <el-icon :size="80" color="#C0C4CC"><Box /></el-icon>
          </template>
          <template #description>
            <p>当前没有需要进货的商品</p>
            <p class="empty-tip">点击上方"生成进货建议"按钮，系统将自动分析库存状态</p>
          </template>
        </el-empty>
      </div>

      <!-- 表格数据 -->
      <el-table v-else :data="paginatedSuggestions" stripe style="width: 100%" show-summary :summary-method="getSummary">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.lightStatus === 1 ? 'danger' : 'warning'" effect="dark" size="small">
              {{ row.lightStatus === 1 ? '红灯' : '黄灯' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品信息" min-width="200">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="product-name">{{ row.productName }}</div>
              <div class="product-meta">
                <span class="product-code">{{ row.productCode }}</span>
                <el-tag size="small" effect="plain">{{ row.categoryName }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="dailySales" label="日均销量" width="100" align="center">
          <template #default="{ row }">
            <span class="daily-sales">{{ row.dailySales ? row.dailySales.toFixed(1) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" label="当前库存" width="100" align="center">
          <template #default="{ row }">
            <span class="stock-warning">{{ row.currentStock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="90" align="center" />
        <el-table-column prop="targetStock" label="目标库存" width="90" align="center" />
        <el-table-column label="建议进货量" width="140" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.purchaseQty"
              :min="1"
              :max="999"
              size="small"
              controls-position="right"
              @change="handleQtyChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="purchasePrice" label="进货单价" width="100" align="center">
          <template #default="{ row }">
            ¥{{ (row.purchasePrice || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="小计" width="110" align="center">
          <template #default="{ row }">
            <span class="subtotal">¥{{ ((row.purchaseQty || 0) * (row.purchasePrice || 0)).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleRemove(row)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="suggestions.length"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>

      <!-- 底部汇总 -->
      <div class="total-bar">
        <div class="total-left">
          <span>已选择 <strong>{{ suggestions.length }}</strong> 种商品进货</span>
        </div>
        <div class="total-right">
          <span class="total-label">合计金额：</span>
          <span class="total-value">¥{{ formatMoney(totalAmount) }}</span>
        </div>
      </div>
    </el-card>

    <!-- 生成进货单弹窗 -->
    <el-dialog v-model="orderDialogVisible" title="确认生成进货单" width="780px" destroy-on-close>
      <el-form :model="orderForm" label-width="100px" class="order-form">
        <el-form-item label="预计到货">
          <el-date-picker
            v-model="orderForm.expectedDate"
            type="date"
            placeholder="选择预计到货日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="2" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>

      <!-- 进货明细表 -->
      <div class="order-detail-title">
        <span>进货明细</span>
        <el-tag type="info" size="small" effect="plain">{{ suggestions.length }} 种商品</el-tag>
      </div>
      <div class="order-detail-table">
        <el-table :data="suggestions" size="small" stripe max-height="280">
          <el-table-column type="index" label="#" width="45" align="center" />
          <el-table-column label="状态" width="65" align="center">
            <template #default="{ row }">
              <el-tag :type="row.lightStatus === 1 ? 'danger' : 'warning'" size="small" effect="dark">
                {{ row.lightStatus === 1 ? '红灯' : '黄灯' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="categoryName" label="分类" width="85" align="center" show-overflow-tooltip />
          <el-table-column label="进货量" width="75" align="center">
            <template #default="{ row }">
              <span class="qty-value">{{ row.purchaseQty }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="80" align="center">
            <template #default="{ row }">
              ¥{{ (row.purchasePrice || 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="小计" width="90" align="center">
            <template #default="{ row }">
              <span class="subtotal-val">¥{{ ((row.purchaseQty || 0) * (row.purchasePrice || 0)).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 汇总栏 -->
      <div class="order-summary-bar">
        <div class="summary-stat">
          <span class="stat-label">商品种类</span>
          <span class="stat-value">{{ suggestions.length }} 种</span>
        </div>
        <div class="summary-stat">
          <span class="stat-label">进货总数</span>
          <span class="stat-value">{{ totalQuantity }} 件</span>
        </div>
        <div class="summary-stat highlight">
          <span class="stat-label">进货总额</span>
          <span class="stat-value">¥{{ formatMoney(totalAmount) }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmGenerateOrder">确认生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Goods, Box, Money, Calendar, Refresh, Download, Warning, AlarmClock } from '@element-plus/icons-vue'
import { generateSuggestions, getSuggestionList, getSuggestionSummary, ignoreSuggestion, adjustSuggestionQuantity } from '@/api/restocking'
import { createPurchaseOrder } from '@/api/restocking'

const router = useRouter()

// 加载状态
const loading = ref(false)

// 预测日期
const predictDate = ref('')

// 灯位筛选：0=全部, 1=仅红灯, 2=仅黄灯
const lightFilter = ref(0)

// 分页配置
const pagination = reactive({
  currentPage: 1,
  pageSize: 10
})

// 统计数据
const redCount = ref(0)
const yellowCount = ref(0)

// 进货建议列表
const suggestions = ref([])

// 分页后的数据（计算属性）
const paginatedSuggestions = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return suggestions.value.slice(start, end)
})

// 进货单弹窗
const orderDialogVisible = ref(false)
const orderForm = reactive({
  expectedDate: '',
  remark: ''
})

// 计算属性
const totalQuantity = computed(() => {
  return suggestions.value.reduce((sum, item) => sum + (item.purchaseQty || 0), 0)
})

const totalAmount = computed(() => {
  return suggestions.value.reduce((sum, item) => sum + (item.purchaseQty || 0) * (item.purchasePrice || 0), 0)
})

// 格式化金额
const formatMoney = (value) => {
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 表格合计
const getSummary = ({ columns }) => {
  const totalPages = Math.ceil(suggestions.value.length / pagination.pageSize)
  const sums = columns.map((column, index) => {
    if (index === 0) return '合计'
    if (column.label === '商品信息') {
      return `共 ${suggestions.value.length} 项，第 ${pagination.currentPage} / ${totalPages} 页`
    }
    if (column.label === '小计') return '¥' + formatMoney(totalAmount.value)
    return ''
  })
  return sums
}

// 获取进货建议
const fetchSuggestions = async () => {
  loading.value = true

  try {
    // 构建查询参数
    const listParams = { status: 0 }
    if (lightFilter.value > 0) {
      listParams.lightStatus = lightFilter.value
    }

    // 并行获取列表和汇总
    const [listRes, summaryRes] = await Promise.all([
      getSuggestionList(listParams),
      getSuggestionSummary()
    ])

    if (listRes.code === 200 && listRes.data) {
      suggestions.value = listRes.data.map(item => ({
        ...item,
        purchaseQty: item.finalQuantity || item.suggestedQuantity
      }))
    }

    if (summaryRes.code === 200 && summaryRes.data) {
      redCount.value = summaryRes.data.redCount || 0
      yellowCount.value = summaryRes.data.yellowCount || 0
    }

  } catch (error) {
    console.error('获取进货建议失败:', error)
    ElMessage.error('获取进货建议失败')
  } finally {
    loading.value = false
  }
}

// 灯位筛选变更
const handleLightFilterChange = () => {
  pagination.currentPage = 1  // 重置到第一页
  fetchSuggestions()
}

// 刷新建议（重新生成）
const handleRefresh = async () => {
  loading.value = true

  try {
    const res = await generateSuggestions()
    if (res.code === 200) {
      pagination.currentPage = 1  // 重置到第一页
      ElMessage.success(res.data.message || '进货建议已更新')
      await fetchSuggestions()
    }
  } catch (error) {
    console.error('生成进货建议失败:', error)
    ElMessage.error('生成进货建议失败')
  } finally {
    loading.value = false
  }
}

// 导出清单（CSV）
const handleExport = () => {
  if (suggestions.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  const headers = ['状态', '商品编码', '商品名称', '分类', '日均销量', '当前库存', '安全库存', '目标库存', '进货数量', '进货单价(元)', '小计(元)']
  const rows = suggestions.value.map(item => [
    item.lightStatus === 1 ? '红灯' : '黄灯',
    item.productCode || '',
    item.productName || '',
    item.categoryName || '',
    item.dailySales ? item.dailySales.toFixed(1) : '0',
    item.currentStock ?? 0,
    item.safetyStock ?? 0,
    item.targetStock ?? 0,
    item.purchaseQty ?? 0,
    (item.purchasePrice || 0).toFixed(2),
    ((item.purchaseQty || 0) * (item.purchasePrice || 0)).toFixed(2)
  ])

  const csvContent = [headers, ...rows]
    .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')

  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const date = new Date().toISOString().slice(0, 10)
  link.href = url
  link.download = `进货建议清单_${date}.csv`
  link.click()
  URL.revokeObjectURL(url)

  ElMessage.success(`已导出 ${suggestions.value.length} 条进货建议`)
}

// 移除商品（忽略建议）
const handleRemove = async (item) => {
  try {
    const res = await ignoreSuggestion(item.id)
    if (res.code === 200) {
      const idx = suggestions.value.findIndex(s => s.id === item.id)
      if (idx !== -1) suggestions.value.splice(idx, 1)
      // 更新统计
      if (item.lightStatus === 1) redCount.value--
      else yellowCount.value--
      ElMessage.success('已移除')
      // 如果当前页数据为空，回到上一页
      if (paginatedSuggestions.value.length === 0 && pagination.currentPage > 1) {
        pagination.currentPage--
      }
    }
  } catch (error) {
    console.error('移除失败:', error)
    ElMessage.error('移除失败')
  }
}

// 分页事件处理
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.currentPage = 1  // 重置到第一页
}

const handleCurrentChange = (val) => {
  pagination.currentPage = val
}

// 修改进货量（持久化到后端）
const handleQtyChange = async (row) => {
  if (!row.id || !row.purchaseQty) return
  try {
    await adjustSuggestionQuantity(row.id, row.purchaseQty)
  } catch (error) {
    ElMessage.error('保存进货量失败')
  }
}

// 生成进货单
const handleGenerateOrder = () => {
  if (suggestions.value.length === 0) {
    ElMessage.warning('暂无需要进货的商品')
    return
  }

  // 设置默认预计到货日期
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  orderForm.expectedDate = tomorrow.toISOString().split('T')[0]

  orderDialogVisible.value = true
}

// 确认生成进货单
const confirmGenerateOrder = async () => {
  if (suggestions.value.length === 0) {
    ElMessage.warning('暂无需要进货的商品')
    return
  }

  try {
    // 构建请求参数
    const orderData = {
      expectedDate: orderForm.expectedDate,
      remark: orderForm.remark,
      items: suggestions.value.map(item => ({
        suggestionId: item.id,
        productId: item.productId,
        quantity: item.purchaseQty
      }))
    }

    const res = await createPurchaseOrder(orderData)

    if (res.code === 200) {
      ElMessage.success(res.data.message || '进货单创建成功！')
      orderDialogVisible.value = false

      // 刷新列表
      await fetchSuggestions()
    } else {
      ElMessage.error(res.message || '创建失败')
    }

  } catch (error) {
    console.error('生成进货单失败:', error)
    ElMessage.error('生成进货单失败')
  }
}

onMounted(() => {
  fetchSuggestions()
})
</script>

<style scoped lang="scss">
.restocking-suggestion {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 100px);
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

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

.summary-row {
  margin-bottom: 20px;

  .summary-card {
    display: flex;
    align-items: center;
    padding: 20px;
    border-radius: 16px;
    color: #fff;

    .summary-icon {
      width: 56px;
      height: 56px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      margin-right: 16px;
    }

    .summary-content {
      .summary-value {
        font-size: 24px;
        font-weight: 800;
        margin-bottom: 4px;
      }
      .summary-label {
        font-size: 14px;
        opacity: 0.9;
      }
    }

    &.summary-blue { background: linear-gradient(135deg, #3B82F6, #60A5FA); }
    &.summary-green { background: linear-gradient(135deg, #10B981, #34D399); }
    &.summary-orange { background: linear-gradient(135deg, #F59E0B, #FBBF24); }
    &.summary-purple { background: linear-gradient(135deg, #8B5CF6, #A78BFA); }
    &.summary-red { background: linear-gradient(135deg, #EF4444, #F87171); }
  }
}

.algorithm-alert {
  margin-bottom: 20px;
  border-radius: 12px;

  .algorithm-detail {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;
    margin-top: 8px;

    .light-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
    }
  }
}

.suggestion-card {
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

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
      font-weight: 700;
      color: #334155;

      .light-filter {
        margin-left: 8px;
        font-weight: 400;
      }
    }

    .header-right {
      display: flex;
      gap: 10px;
    }
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

  .product-cell {
    .product-name {
      font-weight: 600;
      color: #1e293b;
      margin-bottom: 4px;
    }
    .product-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      .product-code {
        font-size: 12px;
        color: #94a3b8;
        font-family: monospace;
      }
    }
  }

  .stock-warning {
    color: #F59E0B;
    font-weight: 600;
  }

  .subtotal {
    font-weight: 600;
    color: #1e293b;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    padding: 20px 0;
    margin-top: 16px;
    border-top: 1px solid #f1f5f9;
  }
}

.total-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8fafc;
  border-radius: 12px;
  margin-top: 16px;

  .total-left {
    color: #64748b;
    strong { color: #1e293b; }
  }

  .total-right {
    display: flex;
    align-items: baseline;
    gap: 8px;

    .total-label {
      color: #64748b;
    }
    .total-value {
      font-size: 28px;
      font-weight: 800;
      color: #EF4444;
    }
  }
}

.order-form {
  margin-bottom: 16px;
}

.order-detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid #3B82F6;
}

.order-detail-table {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f1f5f9;

  .qty-value {
    font-weight: 600;
    color: #3B82F6;
  }

  .subtotal-val {
    font-weight: 600;
    color: #1e293b;
  }
}

.order-summary-bar {
  display: flex;
  justify-content: flex-end;
  gap: 32px;
  background: #f8fafc;
  border-radius: 10px;
  padding: 14px 20px;

  .summary-stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .stat-label {
      font-size: 12px;
      color: #94a3b8;
    }

    .stat-value {
      font-size: 15px;
      font-weight: 700;
      color: #1e293b;
    }

    &.highlight .stat-value {
      font-size: 20px;
      color: #EF4444;
    }
  }
}
</style>