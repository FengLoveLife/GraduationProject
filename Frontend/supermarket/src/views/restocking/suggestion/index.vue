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
        <div class="summary-card summary-blue">
          <div class="summary-icon"><Goods /></div>
          <div class="summary-content">
            <div class="summary-value">{{ suggestions.length }}</div>
            <div class="summary-label">建议进货商品</div>
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
        <div class="summary-card summary-orange">
          <div class="summary-icon"><Money /></div>
          <div class="summary-content">
            <div class="summary-value">¥{{ formatMoney(totalAmount) }}</div>
            <div class="summary-label">预计进货金额</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card summary-purple">
          <div class="summary-icon"><Calendar /></div>
          <div class="summary-content">
            <div class="summary-value">{{ predictDate }}</div>
            <div class="summary-label">预测日期</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 算法说明 -->
    <el-alert type="info" :closable="false" show-icon class="algorithm-alert">
      <template #title>
        <span>进货建议算法：<strong>建议进货量 = 预测销量 + 安全库存 - 当前库存</strong></span>
      </template>
      <template #default>
        系统综合考虑预测销量、安全库存阈值、当前库存状态，自动计算最优进货量
      </template>
    </el-alert>

    <!-- 进货建议表格 -->
    <el-card shadow="hover" class="suggestion-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>进货建议清单</span>
            <el-tag type="warning" effect="plain" size="small">共 {{ suggestions.length }} 项</el-tag>
          </div>
          <div class="header-right">
            <el-button :icon="Refresh" @click="handleRefresh" :loading="loading">刷新建议</el-button>
            <el-button :icon="Download" @click="handleExport">导出清单</el-button>
          </div>
        </div>
      </template>

      <el-table :data="suggestions" stripe style="width: 100%" show-summary :summary-method="getSummary">
        <el-table-column type="index" label="#" width="50" align="center" />
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
        <el-table-column prop="predictedQuantity" label="预测销量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain">{{ row.predictedQuantity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" label="当前库存" width="100" align="center">
          <template #default="{ row }">
            <span class="stock-warning">{{ row.currentStock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="90" align="center" />
        <el-table-column label="缺口" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="danger" effect="dark" size="small">
              -{{ row.predictedQuantity + row.safetyStock - row.currentStock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建议进货量" width="140" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.purchaseQty"
              :min="row.suggestedPurchase"
              :max="999"
              size="small"
              controls-position="right"
            />
          </template>
        </el-table-column>
        <el-table-column prop="purchasePrice" label="进货单价" width="100" align="center">
          <template #default="{ row }">
            ¥{{ row.purchasePrice.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="小计" width="110" align="center">
          <template #default="{ row }">
            <span class="subtotal">¥{{ (row.purchaseQty * row.purchasePrice).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link size="small" @click="handleRemove($index)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
    <el-dialog v-model="orderDialogVisible" title="确认生成进货单" width="550px" destroy-on-close>
      <el-form :model="orderForm" label-width="100px" class="order-form">
        <el-form-item label="供应商">
          <el-input v-model="orderForm.supplier" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="预计到货">
          <el-date-picker
            v-model="orderForm.expectedDate"
            type="date"
            placeholder="选择预计到货日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>

      <div class="order-summary">
        <div class="summary-item">
          <span class="label">进货商品</span>
          <span class="value">{{ suggestions.length }} 种</span>
        </div>
        <div class="summary-item">
          <span class="label">进货总数</span>
          <span class="value">{{ totalQuantity }} 件</span>
        </div>
        <div class="summary-item highlight">
          <span class="label">进货总额</span>
          <span class="value">¥{{ formatMoney(totalAmount) }}</span>
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
import { Document, Goods, Box, Money, Calendar, Refresh, Download } from '@element-plus/icons-vue'

const router = useRouter()

// 加载状态
const loading = ref(false)

// 预测日期
const predictDate = ref('')

// 进货建议列表
const suggestions = ref([])

// 进货单弹窗
const orderDialogVisible = ref(false)
const orderForm = reactive({
  supplier: '',
  expectedDate: '',
  remark: ''
})

// 计算属性
const totalQuantity = computed(() => {
  return suggestions.value.reduce((sum, item) => sum + item.purchaseQty, 0)
})

const totalAmount = computed(() => {
  return suggestions.value.reduce((sum, item) => sum + item.purchaseQty * item.purchasePrice, 0)
})

// 格式化金额
const formatMoney = (value) => {
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 表格合计
const getSummary = ({ columns, data }) => {
  const sums = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (column.label === '小计') {
      sums[index] = '¥' + formatMoney(totalAmount.value)
    } else {
      sums[index] = ''
    }
  })
  return sums
}

// 模拟数据
const mockSuggestions = [
  { productId: 1, productName: '可口可乐500ml', productCode: 'SP001', categoryName: '饮料', predictedQuantity: 45, currentStock: 30, safetyStock: 20, suggestedPurchase: 35, purchasePrice: 2.50, purchaseQty: 35 },
  { productId: 3, productName: '康师傅红烧牛肉面', productCode: 'SP003', categoryName: '方便食品', predictedQuantity: 28, currentStock: 15, safetyStock: 20, suggestedPurchase: 33, purchasePrice: 3.20, purchaseQty: 33 },
  { productId: 4, productName: '旺旺雪饼', productCode: 'SP004', categoryName: '休闲零食', predictedQuantity: 22, currentStock: 18, safetyStock: 10, suggestedPurchase: 14, purchasePrice: 8.50, purchaseQty: 14 },
  { productId: 5, productName: '德芙巧克力', productCode: 'SP005', categoryName: '糖果巧克力', predictedQuantity: 15, currentStock: 8, safetyStock: 10, suggestedPurchase: 17, purchasePrice: 12.00, purchaseQty: 17 },
  { productId: 7, productName: '双汇王中王火腿肠', productCode: 'SP007', categoryName: '肉制品', predictedQuantity: 20, currentStock: 12, safetyStock: 15, suggestedPurchase: 23, purchasePrice: 1.80, purchaseQty: 23 },
  { productId: 9, productName: '奥利奥夹心饼干', productCode: 'SP009', categoryName: '休闲零食', predictedQuantity: 25, currentStock: 10, safetyStock: 15, suggestedPurchase: 30, purchasePrice: 9.50, purchaseQty: 30 },
  { productId: 11, productName: '金龙鱼调和油5L', productCode: 'SP011', categoryName: '粮油调味', predictedQuantity: 8, currentStock: 5, safetyStock: 10, suggestedPurchase: 13, purchasePrice: 65.00, purchaseQty: 13 },
  { productId: 13, productName: '维达抽纸', productCode: 'SP013', categoryName: '日用品', predictedQuantity: 18, currentStock: 6, safetyStock: 12, suggestedPurchase: 24, purchasePrice: 5.80, purchaseQty: 24 },
]

// 获取进货建议
const fetchSuggestions = async () => {
  loading.value = true

  try {
    await new Promise(resolve => setTimeout(resolve, 600))

    suggestions.value = mockSuggestions.map(item => ({
      ...item,
      purchaseQty: item.suggestedPurchase
    }))

    // 设置预测日期
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    predictDate.value = `${tomorrow.getMonth() + 1}/${tomorrow.getDate()}`

  } catch (error) {
    ElMessage.error('获取进货建议失败')
  } finally {
    loading.value = false
  }
}

// 刷新建议
const handleRefresh = () => {
  fetchSuggestions()
}

// 导出清单
const handleExport = () => {
  ElMessage.success('进货清单已导出')
}

// 移除商品
const handleRemove = (index) => {
  suggestions.value.splice(index, 1)
  ElMessage.success('已移除该商品')
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
  try {
    await new Promise(resolve => setTimeout(resolve, 800))

    ElMessage.success('进货单生成成功！')
    orderDialogVisible.value = false

    // 清空列表或跳转
    suggestions.value = []

  } catch (error) {
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
  }
}

.algorithm-alert {
  margin-bottom: 20px;
  border-radius: 12px;
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
    }

    .header-right {
      display: flex;
      gap: 10px;
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
  margin-bottom: 20px;
}

.order-summary {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;

  .summary-item {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px dashed #e4e7ed;

    &:last-child {
      border-bottom: none;
    }

    .label {
      color: #64748b;
    }
    .value {
      font-weight: 600;
      color: #1e293b;
    }

    &.highlight .value {
      font-size: 18px;
      color: #EF4444;
    }
  }
}
</style>