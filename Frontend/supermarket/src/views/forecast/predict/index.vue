<template>
  <div class="forecast-predict">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">销量预测</h2>
        <p class="subtitle">基于 LightGBM 机器学习模型，智能预测商品销量</p>
      </div>
      <div class="header-right">
        <span v-if="lastUpdateTime" class="last-update-tip">
          <el-icon><Clock /></el-icon>
          最后预测于 {{ lastUpdateTime }}
        </span>
        <el-button type="primary" :icon="Cpu" @click="handlePredict" :loading="loading">
          {{ allResults.length > 0 ? '重新预测' : '执行预测' }}
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
        <el-form-item label="预测起始">
          <div class="fixed-date-display">
            <el-icon><Calendar /></el-icon>
            <span class="date-text">{{ predictForm.date }}</span>
            <el-tag type="success" size="small" effect="plain">明天</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="预测天数">
          <el-radio-group v-model="predictForm.days" size="default">
            <el-radio-button :label="1">1 天</el-radio-button>
            <el-radio-button :label="3">3 天</el-radio-button>
            <el-radio-button :label="7">7 天</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-tag type="info" effect="plain">
            预测范围：{{ predictForm.date }} ~ {{ forecastEndDate }}
          </el-tag>
        </el-form-item>
      </el-form>
      <div class="factor-tips">
        <el-icon><InfoFilled /></el-icon>
        <span>
          预测模型将综合考量：<strong>历史销量</strong>、<strong>星期规律</strong>、<strong>节假日效应</strong>、<strong>天气影响</strong> 等因素。
          天气和节假日数据将自动从日历因子获取。
        </span>
      </div>

      <!-- 模型状态指标 -->
      <div class="model-metrics" v-if="modelStatus.loaded">
        <div class="metric-item">
          <span class="metric-label">模型状态</span>
          <el-tag type="success" size="small" effect="dark">已就绪</el-tag>
        </div>
        <div class="metric-item" v-if="modelStatus.mae != null">
          <span class="metric-label">MAE（平均误差）</span>
          <span class="metric-value">{{ modelStatus.mae }} 件</span>
        </div>
        <div class="metric-item" v-if="modelStatus.mape != null">
          <span class="metric-label">MAPE（误差率）</span>
          <span class="metric-value">{{ modelStatus.mape }}%</span>
        </div>
        <div class="metric-item" v-if="modelStatus.r2 != null">
          <span class="metric-label">R²（拟合度）</span>
          <span class="metric-value">{{ modelStatus.r2 }}</span>
        </div>
      </div>
    </el-card>

    <!-- 预测结果统计 -->
    <el-row :gutter="16" class="stat-row" v-if="allResults.length > 0">
      <el-col :span="6">
        <div class="stat-item stat-blue">
          <div class="stat-value">{{ productCount }}</div>
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
          <div class="stat-value">{{ needPurchaseCount }}</div>
          <div class="stat-label">库存紧张</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item stat-red">
          <div class="stat-value">{{ warningCount }}</div>
          <div class="stat-label">库存告急</div>
        </div>
      </el-col>
    </el-row>

    <!-- 结果区域 -->
    <el-card shadow="hover" class="result-card" v-loading="loading" v-if="allResults.length > 0 || loading">
      <template #header>
        <div class="card-header">
          <div class="header-left-group">
            <!-- 视图切换（多天预测时显示） -->
            <el-radio-group v-if="predictForm.days > 1" v-model="viewMode" size="small" class="view-toggle">
              <el-radio-button label="summary">
                <el-icon><DataAnalysis /></el-icon> 按商品汇总
              </el-radio-button>
              <el-radio-button label="daily">
                <el-icon><Calendar /></el-icon> 按天浏览
              </el-radio-button>
            </el-radio-group>
            <span v-else class="card-title">预测结果</span>

            <!-- 日期标签（按天浏览时显示） -->
            <div v-if="viewMode === 'daily' && forecastDates.length > 1" class="date-tabs">
              <el-tag
                v-for="date in forecastDates"
                :key="date"
                :type="selectedDate === date ? 'primary' : 'info'"
                :effect="selectedDate === date ? 'dark' : 'plain'"
                class="date-tag"
                @click="selectedDate = date"
                style="cursor: pointer"
              >
                {{ formatShortDate(date) }}
              </el-tag>
            </div>
          </div>

          <!-- 搜索和筛选 -->
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品名称"
              :prefix-icon="Search"
              clearable
              style="width: 200px; margin-right: 12px"
            />
            <el-select v-model="filterStatus" placeholder="筛选状态" clearable style="width: 130px">
              <el-option label="库存充足" value="sufficient" />
              <el-option label="库存紧张" value="needPurchase" />
              <el-option label="库存告急" value="warning" />
            </el-select>
          </div>
        </div>
      </template>

      <!-- 按商品汇总视图 -->
      <el-table
        v-if="viewMode === 'summary'"
        :data="paginatedSummary"
        stripe
        style="width: 100%"
        @row-click="showDetail"
      >
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
        <el-table-column label="周期预测总量" width="120" align="center">
          <template #default="{ row }">
            <span class="predict-total">{{ row.totalPredicted }}<span class="unit"> 件</span></span>
          </template>
        </el-table-column>
        <el-table-column label="日均预测" width="100" align="center">
          <template #default="{ row }">
            <span class="predict-avg">{{ row.avgPredicted }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" label="当前库存" width="100" align="center">
          <template #default="{ row }">
            <span :class="getStockClass(row)">{{ row.currentStock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">{{ getStatusText(row) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 按天浏览视图 -->
      <el-table
        v-else
        :data="paginatedDaily"
        stripe
        style="width: 100%"
        @row-click="showDetail"
      >
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
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">{{ getStatusText(row) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="viewMode === 'summary' ? filteredSummary.length : filteredDaily.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentProduct?.productName"
      width="650px"
      destroy-on-close
    >
      <div class="detail-content" v-if="currentProduct">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">预测信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="商品名称">{{ currentProduct.productName }}</el-descriptions-item>
            <el-descriptions-item label="商品编码">{{ currentProduct.productCode }}</el-descriptions-item>
            <el-descriptions-item label="商品分类">{{ currentProduct.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="预测周期">
              {{ predictForm.date }} ~ {{ forecastEndDate }}（{{ predictForm.days }} 天）
            </el-descriptions-item>
            <el-descriptions-item label="周期预测总量">
              <el-tag type="primary" size="large">{{ currentProduct.totalPredicted || currentProduct.predictedQuantity }} 件</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentProduct)" size="large">{{ getStatusText(currentProduct) }}</el-tag>
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
              <div class="stock-label">周期预测</div>
              <div class="stock-value primary">{{ currentProduct.totalPredicted || currentProduct.predictedQuantity }}</div>
            </div>
          </div>
        </div>

        <!-- 逐日预测（多天时展示） -->
        <div class="detail-section" v-if="currentProduct.dailyPredictions && currentProduct.dailyPredictions.length > 1">
          <div class="section-title">逐日预测明细</div>
          <div class="daily-pills">
            <div v-for="dp in currentProduct.dailyPredictions" :key="dp.date" class="day-pill">
              <div class="day-date">{{ formatShortDate(dp.date) }}</div>
              <div class="day-value">{{ dp.predictedQuantity }}<span class="day-unit">件</span></div>
            </div>
          </div>
        </div>

        <!-- 历史趋势图（真实数据） -->
        <div class="detail-section">
          <div class="section-title">
            近14天实际销量 &amp; 预测对比
            <el-tag v-if="chartLoading" type="info" size="small" style="margin-left: 8px">加载中...</el-tag>
          </div>
          <div ref="detailChartRef" class="detail-chart"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Cpu, InfoFilled, Search, DataAnalysis, Calendar } from '@element-plus/icons-vue'
import { runForecast, getForecastResults, getForecastVsActual, getForecastStatus } from '@/api/forecast'
import { Clock } from '@element-plus/icons-vue'

const loading = ref(false)
const chartLoading = ref(false)

// 最后预测时间（展示用）
const lastUpdateTime = ref('')

// 模型状态
const modelStatus = reactive({ loaded: false, mae: null, mape: null, r2: null, trainTime: null })

const loadModelStatus = async () => {
  try {
    const res = await getForecastStatus()
    if (res.code === 200 && res.data) {
      const d = res.data
      modelStatus.loaded = d.model_loaded || false
      const stats = d.training_stats || {}
      modelStatus.mae = stats.mae != null ? stats.mae.toFixed(2) : null
      modelStatus.mape = stats.mape != null ? stats.mape.toFixed(1) : null
      modelStatus.r2 = stats.r2 != null ? stats.r2.toFixed(3) : null
      modelStatus.trainTime = d.model_info?.training_time || null
    }
  } catch (e) {
    // 服务未启动时静默失败
  }
}

// 预测表单
const predictForm = reactive({ date: '', days: 7 })

// 今天的日期字符串（用于 offsetDate）
const today = new Date().toISOString().split('T')[0]

// 视图模式：summary（按商品汇总）/ daily（按天浏览）
const viewMode = ref('summary')

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref('')

// 全部原始预测结果（多天时包含所有日期的数据）
const allResults = ref([])

// 分页
const pagination = reactive({ page: 1, size: 10 })

// 详情
const detailVisible = ref(false)
const currentProduct = ref(null)
const detailChartRef = ref(null)
let detailChart = null

// 按天浏览：当前选中日期
const selectedDate = ref('')

// 计算预测日期列表
const forecastDates = computed(() => {
  if (!predictForm.date || allResults.value.length === 0) return []
  const dates = [...new Set(allResults.value.map(r => r.forecastDate))].sort()
  return dates
})

// 预测结束日期
const forecastEndDate = computed(() => {
  if (forecastDates.value.length === 0) return predictForm.date
  return forecastDates.value[forecastDates.value.length - 1]
})

// ===================== 按商品汇总视图 =====================

// 将多天数据按商品聚合
const summaryResults = computed(() => {
  if (allResults.value.length === 0) return []

  const productMap = new Map()
  for (const item of allResults.value) {
    const pid = item.productId
    if (!productMap.has(pid)) {
      productMap.set(pid, {
        productId: pid,
        productCode: item.productCode,
        productName: item.productName,
        categoryId: item.categoryId,
        categoryName: item.categoryName,
        currentStock: item.currentStock,
        safetyStock: item.safetyStock,
        totalPredicted: 0,
        dailyPredictions: [],
        stockStatus: item.stockStatus,
        restockCycleDays: item.restockCycleDays || 7,
        historicalDailyAvg: item.historicalDailyAvg ?? null
      })
    }
    const entry = productMap.get(pid)
    entry.totalPredicted += item.predictedQuantity || 0
    entry.restockCycleDays = item.restockCycleDays || entry.restockCycleDays
    entry.historicalDailyAvg = item.historicalDailyAvg ?? entry.historicalDailyAvg
    entry.dailyPredictions.push({
      date: item.forecastDate,
      predictedQuantity: item.predictedQuantity
    })
  }

  return Array.from(productMap.values()).map(p => {
    const days = p.dailyPredictions.length || 1
    const restockCycle = p.restockCycleDays || 7
    const predictDailyAvg = p.totalPredicted / days
    const histAvg = p.historicalDailyAvg ?? predictDailyAvg

    // 根据补货周期选择日均计算方式
    let effectiveDailyAvg
    if (restockCycle <= 7) {
      // 短周期：频繁进货，完全信任近期预测
      effectiveDailyAvg = predictDailyAvg
    } else if (restockCycle < 15) {
      // 中周期：预测60% + 历史40%
      effectiveDailyAvg = predictDailyAvg * 0.6 + histAvg * 0.4
    } else {
      // 长周期：单次进货量大，历史规律更重要，预测30% + 历史70%
      effectiveDailyAvg = predictDailyAvg * 0.3 + histAvg * 0.7
    }

    // 日均预测展示纯 AI 预测日均（totalPredicted / 预测天数），与周期预测总量保持一致
    p.avgPredicted = Math.round(predictDailyAvg)

    // 三灯策略（统一标准）
    // 红灯：stock <= safetyStock
    // 黄灯：stock <= safetyStock + dailyAvg × cycle × 30%
    const yellowLine = p.safetyStock + effectiveDailyAvg * restockCycle * 0.3
    if (p.currentStock <= p.safetyStock) {
      p.stockStatus = 'warning'      // 红灯：触底，立即补
    } else if (p.currentStock <= yellowLine) {
      p.stockStatus = 'needPurchase' // 黄灯：周期70%已消耗，该下单了
    } else {
      p.stockStatus = 'sufficient'   // 绿灯：充足
    }
    p.dailyPredictions.sort((a, b) => a.date.localeCompare(b.date))
    return p
  })
})

const filteredSummary = computed(() => {
  let list = summaryResults.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(i => i.productName.toLowerCase().includes(kw) || i.productCode.toLowerCase().includes(kw))
  }
  if (filterStatus.value) {
    list = list.filter(i => i.stockStatus === filterStatus.value)
  }
  return list
})

const paginatedSummary = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredSummary.value.slice(start, start + pagination.size)
})

// ===================== 按天浏览视图 =====================

const dailyResults = computed(() => {
  if (!selectedDate.value) return []
  return allResults.value.filter(r => r.forecastDate === selectedDate.value)
})

const filteredDaily = computed(() => {
  let list = dailyResults.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(i => i.productName.toLowerCase().includes(kw) || i.productCode.toLowerCase().includes(kw))
  }
  if (filterStatus.value) {
    list = list.filter(i => i.stockStatus === filterStatus.value)
  }
  return list
})

const paginatedDaily = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredDaily.value.slice(start, start + pagination.size)
})

// ===================== 统计数据（基于汇总视图） =====================

const productCount = computed(() => summaryResults.value.length)

const sufficientCount = computed(() =>
  summaryResults.value.filter(i => i.stockStatus === 'sufficient').length
)
const warningCount = computed(() =>
  summaryResults.value.filter(i => i.stockStatus === 'warning').length
)
const needPurchaseCount = computed(() =>
  summaryResults.value.filter(i => i.stockStatus === 'needPurchase').length
)

// ===================== 样式工具函数 =====================

const getStockClass = (row) => {
  if (row.currentStock < row.safetyStock) return 'stock-danger'
  const predicted = row.totalPredicted || row.predictedQuantity || 0
  if (row.currentStock < predicted) return 'stock-warning'
  return 'stock-normal'
}

const getStatusType = (row) => {
  if (row.stockStatus === 'warning') return 'danger'       // 红灯
  if (row.stockStatus === 'needPurchase') return 'warning' // 黄灯
  return 'success'                                         // 绿灯
}

const getStatusText = (row) => {
  if (row.stockStatus === 'warning') return '库存告急'
  if (row.stockStatus === 'needPurchase') return '库存紧张'
  return '库存充足'
}

const formatShortDate = (dateStr) => {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  return `${parseInt(parts[1])}/${parseInt(parts[2])}`
}

// ===================== 执行预测 =====================

const handlePredict = async () => {
  if (!predictForm.date) {
    ElMessage.warning('请选择预测起始日期')
    return
  }

  loading.value = true
  allResults.value = []
  pagination.page = 1

  try {
    await runAndLoadResults(predictForm.date, predictForm.days)
    ElMessage.success(`预测完成，共 ${productCount.value} 个商品，${predictForm.days} 天预测数据`)
  } catch (error) {
    console.error('预测失败:', error)
    ElMessage.error('预测失败，请检查后端服务是否正常')
  } finally {
    loading.value = false
  }
}

// ===================== 详情弹窗 =====================

const showDetail = (row) => {
  // 汇总视图行 row 已经包含 dailyPredictions；按天视图需要从 summaryResults 里找
  let detail = row
  if (!row.dailyPredictions) {
    const found = summaryResults.value.find(s => s.productId === row.productId)
    detail = found ? { ...row, ...found } : row
  }
  currentProduct.value = detail
  detailVisible.value = true
  nextTick(() => renderDetailChart())
}

// 渲染详情图表（真实数据）
const renderDetailChart = async () => {
  if (!detailChartRef.value || !currentProduct.value) return

  chartLoading.value = true
  if (!detailChart) {
    detailChart = echarts.init(detailChartRef.value)
  }

  try {
    // 取过去 14 天 + 预测期 的对比数据
    const histStart = offsetDate(predictForm.date, -14)
    const histEnd = forecastEndDate.value

    const res = await getForecastVsActual({
      startDate: histStart,
      endDate: histEnd,
      productId: currentProduct.value.productId
    })

    const data = (res.data || []).sort((a, b) => a.date.localeCompare(b.date))
    const dates = data.map(d => formatShortDate(d.date))
    const actualSeries = data.map(d => (d.actualQuantity > 0 ? d.actualQuantity : null))
    const predictedSeries = data.map(d => (d.predictedQuantity > 0 ? d.predictedQuantity : null))

    // 找到预测起始位置，用于添加分割线
    const splitIdx = data.findIndex(d => d.date >= predictForm.date)

    const option = {
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          let html = `<b>${params[0]?.axisValue}</b><br/>`
          params.forEach(p => {
            if (p.value !== null && p.value !== undefined) {
              html += `${p.marker}${p.seriesName}：<b>${p.value} 件</b><br/>`
            }
          })
          return html
        }
      },
      legend: { data: ['实际销量', '预测销量'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#e4e7ed' } },
        axisLabel: { color: '#909399', fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisLabel: { color: '#909399' },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
      },
      series: [
        {
          name: '实际销量',
          type: 'line',
          data: actualSeries,
          smooth: true,
          connectNulls: false,
          lineStyle: { color: '#409EFF', width: 2 },
          itemStyle: { color: '#409EFF' },
          symbol: 'circle', symbolSize: 6,
          // 预测起始分割线
          markLine: splitIdx >= 0 ? {
            symbol: 'none',
            data: [{ xAxis: splitIdx, label: { formatter: '预测开始', color: '#F59E0B' } }],
            lineStyle: { color: '#F59E0B', type: 'dashed', width: 1.5 }
          } : {}
        },
        {
          name: '预测销量',
          type: 'line',
          data: predictedSeries,
          smooth: true,
          connectNulls: false,
          lineStyle: { color: '#67C23A', width: 2, type: 'dashed' },
          itemStyle: { color: '#67C23A' },
          symbol: 'diamond', symbolSize: 7
        }
      ]
    }

    detailChart.setOption(option)
  } catch (e) {
    console.error('加载历史对比数据失败:', e)
    ElMessage.warning('历史对比数据加载失败')
  } finally {
    chartLoading.value = false
  }
}

// 日期偏移工具
const offsetDate = (dateStr, days) => {
  const d = new Date(dateStr)
  d.setDate(d.getDate() + days)
  return d.toISOString().split('T')[0]
}

// 切换视图时重置分页
watch(viewMode, () => { pagination.page = 1 })
watch(selectedDate, () => { pagination.page = 1 })
watch(searchKeyword, () => { pagination.page = 1 })
watch(filterStatus, () => { pagination.page = 1 })

// 关闭弹窗时销毁图表
watch(detailVisible, (val) => {
  if (!val && detailChart) {
    detailChart.dispose()
    detailChart = null
  }
})

// 自动检测并加载已有预测数据（页面打开时调用）
const autoLoadOrPredict = async () => {
  const tomorrow = offsetDate(today, 1)
  predictForm.date = tomorrow
  predictForm.days = 7

  loading.value = true
  try {
    // 先查是否已有预测数据
    const res = await getForecastResults({ forecastDate: tomorrow, days: 7 })
    if (res.code === 200 && res.data && res.data.length > 0) {
      // 有数据：直接展示，不重新预测
      allResults.value = res.data
      if (forecastDates.value.length > 0) selectedDate.value = forecastDates.value[0]
      viewMode.value = 'summary'
      // 取最新记录的 createTime 作为展示
      const times = res.data
        .map(r => r.createTime || '')
        .filter(t => t)
        .sort()
      if (times.length > 0) {
        const latestTime = times[times.length - 1]
        // 只显示时分
        lastUpdateTime.value = latestTime.length >= 16 ? latestTime.substring(11, 16) : latestTime
      }
    } else {
      // 没有数据：自动触发预测
      ElMessage.info('正在自动执行今日预测...')
      await runAndLoadResults(tomorrow, 7)
    }
  } catch (e) {
    console.error('自动加载预测数据失败:', e)
  } finally {
    loading.value = false
  }
}

// 执行预测并加载结果（抽取为共用方法）
const runAndLoadResults = async (startDate, days) => {
  const runRes = await runForecast({ forecastStart: startDate, forecastDays: days })
  if (runRes.code !== 200) {
    ElMessage.error(runRes.message || '预测执行失败')
    return
  }
  const resultsRes = await getForecastResults({ forecastDate: startDate, days })
  if (resultsRes.code === 200 && resultsRes.data) {
    allResults.value = resultsRes.data
    if (forecastDates.value.length > 0) selectedDate.value = forecastDates.value[0]
    viewMode.value = days > 1 ? 'summary' : 'daily'
    const now = new Date()
    lastUpdateTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  }
}

onMounted(() => {
  loadModelStatus()
  autoLoadOrPredict()
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

  .title { font-size: 24px; font-weight: 800; color: #1e293b; margin: 0; }
  .subtitle { font-size: 14px; color: #64748b; margin: 8px 0 0; }

  .header-right {
    display: flex; align-items: center; gap: 12px;
    .last-update-tip {
      display: flex; align-items: center; gap: 4px;
      font-size: 13px; color: #94a3b8;
    }
  }
}

.config-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);

  :deep(.el-card__header) { border-bottom: 1px solid #f1f5f9; padding: 16px 20px; }

  .card-header {
    display: flex; justify-content: space-between; align-items: center;
    font-weight: 700; color: #334155;
  }
  .fixed-date-display {
    display: flex; align-items: center; gap: 8px;
    padding: 7px 12px; background: #f1f5f9; border-radius: 6px;
    border: 1px solid #e2e8f0;
    .date-text { font-size: 14px; font-weight: 600; color: #1e293b; }
    .el-icon { color: #64748b; }
  }

  .factor-tips {
    display: flex; align-items: center; gap: 8px;
    padding: 12px 16px; background: #f0f9ff; border-radius: 8px;
    color: #0369a1; font-size: 13px; margin-top: 16px;
  }

  .model-metrics {
    display: flex; align-items: center; gap: 24px; flex-wrap: wrap;
    padding: 12px 16px; background: #f0fdf4; border-radius: 8px;
    margin-top: 12px; border: 1px solid #bbf7d0;

    .metric-item {
      display: flex; align-items: center; gap: 8px;
      .metric-label { font-size: 12px; color: #16a34a; }
      .metric-value { font-size: 14px; font-weight: 700; color: #15803d; }
    }
  }
}

.stat-row {
  margin-bottom: 20px;

  .stat-item {
    padding: 20px; border-radius: 12px; text-align: center;
    .stat-value { font-size: 28px; font-weight: 800; margin-bottom: 4px; }
    .stat-label { font-size: 14px; opacity: 0.9; }
    &.stat-blue { background: linear-gradient(135deg, #3B82F6, #60A5FA); color: #fff; }
    &.stat-green { background: linear-gradient(135deg, #10B981, #34D399); color: #fff; }
    &.stat-orange { background: linear-gradient(135deg, #F59E0B, #FBBF24); color: #fff; }
    &.stat-red { background: linear-gradient(135deg, #EF4444, #F87171); color: #fff; }
  }
}

.result-card {
  border-radius: 16px; border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);

  :deep(.el-card__header) { border-bottom: 1px solid #f1f5f9; padding: 16px 20px; }

  .card-header {
    display: flex; justify-content: space-between; align-items: center;

    .header-left-group {
      display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
      .card-title { font-size: 16px; font-weight: 700; color: #334155; }
    }
    .header-actions { display: flex; align-items: center; }
  }

  .view-toggle { flex-shrink: 0; }

  .date-tabs {
    display: flex; flex-wrap: wrap; gap: 6px;
    .date-tag { font-size: 13px; transition: all 0.2s; }
  }

  .product-cell {
    .product-name { font-weight: 600; color: #1e293b; }
    .product-code { font-size: 12px; color: #94a3b8; font-family: monospace; }
  }

  .date-text { font-size: 13px; color: #64748b; }
  .predict-value { font-weight: 700; color: #3B82F6; font-size: 15px; }
  .predict-total { font-weight: 800; color: #3B82F6; font-size: 16px; .unit { font-size: 12px; color: #94a3b8; } }
  .predict-avg { font-weight: 600; color: #64748b; font-size: 14px; }

  .stock-normal { color: #10B981; font-weight: 600; }
  .stock-warning { color: #F59E0B; font-weight: 600; }
  .stock-danger { color: #EF4444; font-weight: 600; }
  .no-need { color: #94a3b8; }
}

.pagination-wrapper {
  margin-top: 20px; display: flex; justify-content: flex-end;
}

.detail-content {
  .detail-section {
    margin-bottom: 24px;
    .section-title {
      font-size: 15px; font-weight: 700; color: #1e293b;
      margin-bottom: 12px; padding-left: 8px;
      border-left: 3px solid #3B82F6;
      display: flex; align-items: center;
    }
  }

  .stock-analysis {
    display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
    .stock-item {
      text-align: center; padding: 16px; background: #f8fafc; border-radius: 12px;
      .stock-label { font-size: 12px; color: #64748b; margin-bottom: 8px; }
      .stock-value { font-size: 24px; font-weight: 800; color: #1e293b;
        &.primary { color: #3B82F6; }
        &.warning { color: #F59E0B; }
      }
    }
  }

  .daily-pills {
    display: flex; flex-wrap: wrap; gap: 10px;
    .day-pill {
      padding: 10px 16px; background: #EFF6FF; border-radius: 10px;
      text-align: center; min-width: 64px;
      .day-date { font-size: 12px; color: #64748b; margin-bottom: 4px; }
      .day-value { font-size: 18px; font-weight: 700; color: #3B82F6;
        .day-unit { font-size: 11px; color: #94a3b8; }
      }
    }
  }

  .detail-chart { height: 220px; }
}
</style>
