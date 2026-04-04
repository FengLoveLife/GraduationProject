<template>
  <div class="forecast-predict">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">销量预测</h2>
        <p class="subtitle">基于 LightGBM 机器学习模型，智能预测商品销量</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Cpu" @click="handlePredict" :loading="loading">
          执行预测
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
        <el-form-item label="预测起始日期">
          <el-date-picker
            v-model="predictForm.date"
            type="date"
            placeholder="选择预测起始日期"
            value-format="YYYY-MM-DD"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="预测天数">
          <el-select v-model="predictForm.days" placeholder="选择天数" style="width: 120px">
            <el-option label="1天" :value="1" />
            <el-option label="3天" :value="3" />
            <el-option label="7天" :value="7" />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 预测因子说明 -->
      <div class="factor-tips">
        <el-icon><InfoFilled /></el-icon>
        <span>
          预测模型将综合考量：<strong>历史销量</strong>、<strong>星期规律</strong>、<strong>节假日效应</strong>、<strong>天气影响</strong> 等因素。
          天气和节假日数据将自动从日历因子获取。
        </span>
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

      <el-table :data="paginatedResults" stripe style="width: 100%" @row-click="showDetail">
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
        <el-table-column prop="forecastDate" label="预测日期" width="110" align="center">
          <template #default="{ row }">
            <span class="date-text">{{ row.forecastDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="predictedQuantity" label="预测销量" width="100" align="center">
          <template #default="{ row }">
            <span class="predict-value">{{ row.predictedQuantity }}</span>
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
              <el-tag type="danger" effect="dark" size="small">
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
          :page-sizes="[10, 20, 50, 100]"
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
            <el-descriptions-item label="预测日期">{{ currentProduct.forecastDate }}</el-descriptions-item>
            <el-descriptions-item label="预测销量">
              <el-tag type="primary" size="large">{{ currentProduct.predictedQuantity }} 件</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentProduct)" size="large">
                {{ getStatusText(currentProduct) }}
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
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Cpu, InfoFilled, Search } from '@element-plus/icons-vue'
import { runForecast, getForecastResults, getForecastTrend } from '@/api/forecast'

// 加载状态
const loading = ref(false)

// 预测表单
const predictForm = reactive({
  date: '',
  days: 7
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
  predictResults.value.filter(item => item.stockStatus === 'sufficient').length
)
const warningCount = computed(() =>
  predictResults.value.filter(item => item.stockStatus === 'warning').length
)
const needPurchaseCount = computed(() =>
  predictResults.value.filter(item => item.stockStatus === 'needPurchase').length
)

// 筛选后的结果
const filteredResults = computed(() => {
  let results = predictResults.value

  if (searchKeyword.value) {
    results = results.filter(item =>
      item.productName.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      item.productCode.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }

  if (filterStatus.value) {
    results = results.filter(item => item.stockStatus === filterStatus.value)
  }

  return results
})

// 分页后的结果
const paginatedResults = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredResults.value.slice(start, end)
})

// 获取库存状态样式
const getStockClass = (row) => {
  if (row.currentStock < row.safetyStock) return 'stock-danger'
  if (row.currentStock < row.predictedQuantity) return 'stock-warning'
  return 'stock-normal'
}

// 获取状态类型
const getStatusType = (row) => {
  if (row.stockStatus === 'needPurchase') return 'danger'
  if (row.stockStatus === 'warning') return 'warning'
  return 'success'
}

// 获取状态文本
const getStatusText = (row) => {
  if (row.stockStatus === 'needPurchase') return '需要补货'
  if (row.stockStatus === 'warning') return '库存紧张'
  return '库存充足'
}

// 执行预测（调用真实 API）
const handlePredict = async () => {
  if (!predictForm.date) {
    ElMessage.warning('请选择预测起始日期')
    return
  }

  loading.value = true
  predictResults.value = []
  pagination.page = 1

  try {
    // Step 1: 调用后端执行预测
    const runRes = await runForecast({
      forecastStart: predictForm.date,
      forecastDays: predictForm.days
    })

    if (runRes.code !== 200) {
      ElMessage.error(runRes.message || '预测执行失败')
      return
    }

    // Step 2: 获取预测结果
    const resultsRes = await getForecastResults({
      forecastDate: predictForm.date,
      days: predictForm.days
    })

    if (resultsRes.code === 200 && resultsRes.data) {
      predictResults.value = resultsRes.data
      ElMessage.success(`预测完成，共 ${resultsRes.data.length} 条预测数据`)
    } else {
      ElMessage.warning('暂无预测结果')
    }

  } catch (error) {
    console.error('预测失败:', error)
    ElMessage.error('预测失败，请检查后端服务是否正常')
  } finally {
    loading.value = false
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
  const dates = []
  const today = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }

  const baseQty = currentProduct.value.predictedQuantity
  const actual = [
    baseQty - 5 + Math.floor(Math.random() * 3),
    baseQty - 2 + Math.floor(Math.random() * 3),
    baseQty + 3 - Math.floor(Math.random() * 3),
    baseQty - 1 + Math.floor(Math.random() * 3),
    baseQty + 5 - Math.floor(Math.random() * 3),
    baseQty + 2 - Math.floor(Math.random() * 3),
    null
  ]
  const predicted = [
    baseQty - 3, baseQty, baseQty + 2, baseQty - 2,
    baseQty + 3, baseQty + 1, currentProduct.value.predictedQuantity
  ]

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
  // 设置默认日期
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

  .date-text {
    font-size: 13px;
    color: #64748b;
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