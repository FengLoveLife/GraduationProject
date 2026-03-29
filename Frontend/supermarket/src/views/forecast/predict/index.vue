<template>
  <div class="forecast-predict">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">销量预测</h2>
        <p class="subtitle">选择日期和商品，执行智能预测分析</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Cpu" @click="handleBatchPredict" :loading="batchLoading">
          批量预测所有商品
        </el-button>
      </div>
    </div>

    <!-- 预测配置卡片 -->
    <el-card shadow="hover" class="config-card">
      <template #header>
        <div class="card-header">
          <span>预测配置</span>
          <el-tag type="info" size="small">LightGBM 模型</el-tag>
        </div>
      </template>
      <el-form :inline="true" :model="predictForm" class="config-form">
        <el-form-item label="预测日期">
          <el-date-picker
            v-model="predictForm.date"
            type="date"
            placeholder="选择预测日期"
            :disabled-date="disabledDate"
            value-format="YYYY-MM-DD"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="天气条件">
          <el-select v-model="predictForm.weather" placeholder="选择天气" style="width: 140px">
            <el-option label="晴天" :value="0" />
            <el-option label="多云" :value="1" />
            <el-option label="雨天" :value="2" />
            <el-option label="雪天" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否节假日">
          <el-switch v-model="predictForm.isHoliday" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Aim" @click="handlePredict" :loading="loading">
            执行预测
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 预测因子说明 -->
      <div class="factor-tips">
        <el-icon><InfoFilled /></el-icon>
        <span>预测模型将综合考量：<strong>历史销量</strong>、<strong>星期规律</strong>、<strong>节假日效应</strong>、<strong>天气影响</strong> 等因素</span>
      </div>
    </el-card>

    <!-- 预测结果统计 -->
    <el-row :gutter="16" class="stat-row" v-if="predictResults.length > 0">
      <el-col :span="6">
        <div class="stat-item stat-blue">
          <div class="stat-value">{{ predictResults.length }}</div>
          <div class="stat-label">预测商品数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item stat-green">
          <div class="stat-value">{{ sufficientCount }}</div>
          <div class="stat-label">库存充足</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item stat-orange">
          <div class="stat-value">{{ warningCount }}</div>
          <div class="stat-label">库存紧张</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item stat-red">
          <div class="stat-value">{{ needPurchaseCount }}</div>
          <div class="stat-label">需要补货</div>
        </div>
      </el-col>
    </el-row>

    <!-- 预测结果表格 -->
    <el-card shadow="hover" class="result-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>预测结果</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品名称"
              :prefix-icon="Search"
              clearable
              style="width: 200px; margin-right: 12px"
            />
            <el-select v-model="filterStatus" placeholder="筛选状态" clearable style="width: 120px">
              <el-option label="库存充足" value="sufficient" />
              <el-option label="库存紧张" value="warning" />
              <el-option label="需要补货" value="needPurchase" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table :data="filteredResults" stripe style="width: 100%" @row-click="showDetail">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="productName" label="商品名称" min-width="180">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="product-name">{{ row.productName }}</div>
              <div class="product-code">{{ row.productCode }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.categoryName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="predictedQuantity" label="预测销量" width="100" align="center">
          <template #default="{ row }">
            <span class="predict-value">{{ row.predictedQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="100" align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="row.confidence"
              :stroke-width="8"
              :color="getConfidenceColor(row.confidence)"
              :show-text="false"
              style="width: 60px; display: inline-block;"
            />
            <span style="margin-left: 8px; font-size: 12px;">{{ row.confidence }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" label="当前库存" width="100" align="center">
          <template #default="{ row }">
            <span :class="getStockClass(row)">{{ row.currentStock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="90" align="center" />
        <el-table-column prop="suggestedPurchase" label="建议进货" width="100" align="center">
          <template #default="{ row }">
            <template v-if="row.suggestedPurchase > 0">
              <el-tag type="warning" effect="dark" size="small">
                +{{ row.suggestedPurchase }}
              </el-tag>
            </template>
            <template v-else>
              <span class="no-need">-</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="filteredResults.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentProduct?.productName" width="600px" destroy-on-close>
      <div class="detail-content" v-if="currentProduct">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">预测信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="商品名称">{{ currentProduct.productName }}</el-descriptions-item>
            <el-descriptions-item label="商品编码">{{ currentProduct.productCode }}</el-descriptions-item>
            <el-descriptions-item label="商品分类">{{ currentProduct.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="预测日期">{{ predictForm.date }}</el-descriptions-item>
            <el-descriptions-item label="预测销量">
              <el-tag type="primary" size="large">{{ currentProduct.predictedQuantity }} 件</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="置信度">
              <el-tag :type="currentProduct.confidence >= 80 ? 'success' : 'warning'" size="large">
                {{ currentProduct.confidence }}%
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 库存分析 -->
        <div class="detail-section">
          <div class="section-title">库存分析</div>
          <div class="stock-analysis">
            <div class="stock-item">
              <div class="stock-label">当前库存</div>
              <div class="stock-value">{{ currentProduct.currentStock }}</div>
            </div>
            <div class="stock-item">
              <div class="stock-label">安全库存</div>
              <div class="stock-value">{{ currentProduct.safetyStock }}</div>
            </div>
            <div class="stock-item">
              <div class="stock-label">预测销量</div>
              <div class="stock-value primary">{{ currentProduct.predictedQuantity }}</div>
            </div>
            <div class="stock-item">
              <div class="stock-label">建议进货</div>
              <div class="stock-value warning">+{{ currentProduct.suggestedPurchase }}</div>
            </div>
          </div>
        </div>

        <!-- 趋势图 -->
        <div class="detail-section">
          <div class="section-title">近7天销量趋势</div>
          <div ref="detailChartRef" class="detail-chart"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Cpu, Aim, InfoFilled, Search } from '@element-plus/icons-vue'

const router = useRouter()

// 加载状态
const loading = ref(false)
const batchLoading = ref(false)

// 预测表单
const predictForm = reactive({
  date: '',
  weather: 0,
  isHoliday: false
})

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref('')

// 预测结果
const predictResults = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 10
})

// 详情
const detailVisible = ref(false)
const currentProduct = ref(null)
const detailChartRef = ref(null)

// 统计数据
const sufficientCount = computed(() =>
  predictResults.value.filter(item => item.currentStock >= item.predictedQuantity && item.currentStock >= item.safetyStock).length
)
const warningCount = computed(() =>
  predictResults.value.filter(item => item.currentStock < item.safetyStock && item.currentStock >= item.predictedQuantity).length
)
const needPurchaseCount = computed(() =>
  predictResults.value.filter(item => item.suggestedPurchase > 0).length
)

// 筛选后的结果
const filteredResults = computed(() => {
  let results = predictResults.value

  if (searchKeyword.value) {
    results = results.filter(item =>
      item.productName.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }

  if (filterStatus.value) {
    if (filterStatus.value === 'sufficient') {
      results = results.filter(item => item.currentStock >= item.predictedQuantity && item.currentStock >= item.safetyStock)
    } else if (filterStatus.value === 'warning') {
      results = results.filter(item => item.currentStock < item.safetyStock && item.currentStock >= item.predictedQuantity)
    } else if (filterStatus.value === 'needPurchase') {
      results = results.filter(item => item.suggestedPurchase > 0)
    }
  }

  return results
})

// 模拟预测数据
const mockPredictData = [
  { productId: 1, productName: '可口可乐500ml', productCode: 'SP001', categoryName: '饮料', predictedQuantity: 45, confidence: 92, currentStock: 30, safetyStock: 20, suggestedPurchase: 35 },
  { productId: 2, productName: '农夫山泉550ml', productCode: 'SP002', categoryName: '饮料', predictedQuantity: 38, confidence: 88, currentStock: 50, safetyStock: 15, suggestedPurchase: 0 },
  { productId: 3, productName: '康师傅红烧牛肉面', productCode: 'SP003', categoryName: '方便食品', predictedQuantity: 28, confidence: 85, currentStock: 15, safetyStock: 20, suggestedPurchase: 33 },
  { productId: 4, productName: '旺旺雪饼', productCode: 'SP004', categoryName: '休闲零食', predictedQuantity: 22, confidence: 79, currentStock: 18, safetyStock: 10, suggestedPurchase: 14 },
  { productId: 5, productName: '德芙巧克力', productCode: 'SP005', categoryName: '糖果巧克力', predictedQuantity: 15, confidence: 76, currentStock: 8, safetyStock: 10, suggestedPurchase: 17 },
  { productId: 6, productName: '蒙牛纯牛奶250ml', productCode: 'SP006', categoryName: '乳制品', predictedQuantity: 35, confidence: 91, currentStock: 40, safetyStock: 25, suggestedPurchase: 0 },
  { productId: 7, productName: '双汇王中王火腿肠', productCode: 'SP007', categoryName: '肉制品', predictedQuantity: 20, confidence: 83, currentStock: 12, safetyStock: 15, suggestedPurchase: 23 },
  { productId: 8, productName: '乐事薯片原味', productCode: 'SP008', categoryName: '休闲零食', predictedQuantity: 18, confidence: 81, currentStock: 25, safetyStock: 10, suggestedPurchase: 0 },
  { productId: 9, productName: '奥利奥夹心饼干', productCode: 'SP009', categoryName: '休闲零食', predictedQuantity: 25, confidence: 87, currentStock: 10, safetyStock: 15, suggestedPurchase: 30 },
  { productId: 10, productName: '加多宝凉茶', productCode: 'SP010', categoryName: '饮料', predictedQuantity: 32, confidence: 89, currentStock: 45, safetyStock: 20, suggestedPurchase: 0 },
  { productId: 11, productName: '金龙鱼调和油5L', productCode: 'SP011', categoryName: '粮油调味', predictedQuantity: 8, confidence: 75, currentStock: 5, safetyStock: 10, suggestedPurchase: 13 },
  { productId: 12, productName: '海天酱油500ml', productCode: 'SP012', categoryName: '粮油调味', predictedQuantity: 12, confidence: 82, currentStock: 20, safetyStock: 10, suggestedPurchase: 0 },
]

// 禁用过去的日期
const disabledDate = (date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

// 获取置信度颜色
const getConfidenceColor = (confidence) => {
  if (confidence >= 85) return '#67C23A'
  if (confidence >= 70) return '#E6A23C'
  return '#F56C6C'
}

// 获取库存状态样式
const getStockClass = (row) => {
  if (row.currentStock < row.safetyStock) return 'stock-danger'
  if (row.currentStock < row.predictedQuantity) return 'stock-warning'
  return 'stock-normal'
}

// 获取状态类型
const getStatusType = (row) => {
  if (row.suggestedPurchase > 0) return 'danger'
  if (row.currentStock < row.safetyStock) return 'warning'
  return 'success'
}

// 获取状态文本
const getStatusText = (row) => {
  if (row.suggestedPurchase > 0) return '需要补货'
  if (row.currentStock < row.safetyStock) return '库存紧张'
  return '库存充足'
}

// 执行预测
const handlePredict = async () => {
  if (!predictForm.date) {
    ElMessage.warning('请选择预测日期')
    return
  }

  loading.value = true
  predictResults.value = []

  try {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 使用模拟数据，添加随机波动
    predictResults.value = mockPredictData.map(item => ({
      ...item,
      predictedQuantity: Math.round(item.predictedQuantity * (0.9 + Math.random() * 0.2)),
      confidence: Math.round(item.confidence * (0.95 + Math.random() * 0.1))
    })).map(item => ({
      ...item,
      suggestedPurchase: Math.max(0, item.predictedQuantity + item.safetyStock - item.currentStock)
    }))

    ElMessage.success('预测完成')
  } catch (error) {
    ElMessage.error('预测失败，请重试')
  } finally {
    loading.value = false
  }
}

// 批量预测
const handleBatchPredict = async () => {
  if (!predictForm.date) {
    ElMessage.warning('请先选择预测日期')
    return
  }

  batchLoading.value = true

  try {
    await new Promise(resolve => setTimeout(resolve, 1500))

    predictResults.value = mockPredictData.map(item => ({
      ...item,
      predictedQuantity: Math.round(item.predictedQuantity * (0.9 + Math.random() * 0.2)),
      confidence: Math.round(item.confidence * (0.95 + Math.random() * 0.1))
    })).map(item => ({
      ...item,
      suggestedPurchase: Math.max(0, item.predictedQuantity + item.safetyStock - item.currentStock)
    }))

    ElMessage.success('批量预测完成，共预测 ' + predictResults.value.length + ' 个商品')
  } catch (error) {
    ElMessage.error('批量预测失败')
  } finally {
    batchLoading.value = false
  }
}

// 显示详情
const showDetail = (row) => {
  currentProduct.value = row
  detailVisible.value = true

  nextTick(() => {
    renderDetailChart()
  })
}

// 渲染详情图表
const renderDetailChart = () => {
  if (!detailChartRef.value) return

  const chart = echarts.init(detailChartRef.value)

  // 生成模拟趋势数据
  const dates = ['03-15', '03-16', '03-17', '03-18', '03-19', '03-20', '03-21']
  const actual = [42, 38, 45, 40, 48, 52, null]
  const predicted = [40, 41, 43, 42, 50, 49, currentProduct.value.predictedQuantity]

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['实际销量', '预测销量'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#e4e7ed' } } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } } },
    series: [
      {
        name: '实际销量',
        type: 'line',
        data: actual,
        smooth: true,
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '预测销量',
        type: 'line',
        data: predicted,
        smooth: true,
        lineStyle: { color: '#67C23A', width: 2, type: 'dashed' },
        itemStyle: { color: '#67C23A' }
      }
    ]
  }

  chart.setOption(option)
}

// 初始化
onMounted(() => {
  // 设置默认日期为明天
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  predictForm.date = tomorrow.toISOString().split('T')[0]
})
</script>

<style scoped lang="scss">
.forecast-predict {
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

.config-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 700;
    color: #334155;
  }

  .config-form {
    margin-bottom: 0;
  }

  .factor-tips {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #f0f9ff;
    border-radius: 8px;
    color: #0369a1;
    font-size: 13px;
    margin-top: 16px;
  }
}

.stat-row {
  margin-bottom: 20px;

  .stat-item {
    padding: 20px;
    border-radius: 12px;
    text-align: center;

    .stat-value {
      font-size: 28px;
      font-weight: 800;
      margin-bottom: 4px;
    }
    .stat-label {
      font-size: 14px;
      opacity: 0.9;
    }

    &.stat-blue { background: linear-gradient(135deg, #3B82F6, #60A5FA); color: #fff; }
    &.stat-green { background: linear-gradient(135deg, #10B981, #34D399); color: #fff; }
    &.stat-orange { background: linear-gradient(135deg, #F59E0B, #FBBF24); color: #fff; }
    &.stat-red { background: linear-gradient(135deg, #EF4444, #F87171); color: #fff; }
  }
}

.result-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 700;
    color: #334155;

    .header-actions {
      display: flex;
      align-items: center;
    }
  }

  .product-cell {
    .product-name {
      font-weight: 600;
      color: #1e293b;
    }
    .product-code {
      font-size: 12px;
      color: #94a3b8;
      font-family: monospace;
    }
  }

  .predict-value {
    font-weight: 700;
    color: #3B82F6;
  }

  .stock-normal { color: #10B981; font-weight: 600; }
  .stock-warning { color: #F59E0B; font-weight: 600; }
  .stock-danger { color: #EF4444; font-weight: 600; }

  .no-need { color: #94a3b8; }
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

  .stock-analysis {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    .stock-item {
      text-align: center;
      padding: 16px;
      background: #f8fafc;
      border-radius: 12px;

      .stock-label {
        font-size: 12px;
        color: #64748b;
        margin-bottom: 8px;
      }
      .stock-value {
        font-size: 24px;
        font-weight: 800;
        color: #1e293b;

        &.primary { color: #3B82F6; }
        &.warning { color: #F59E0B; }
      }
    }
  }

  .detail-chart {
    height: 200px;
  }
}
</style>