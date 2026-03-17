<template>
  <div class="app-container">
    <!-- Row 1: Global Date Controller -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-header">
        <div class="filter-left">
          <el-icon class="filter-icon"><Calendar /></el-icon>
          <span class="filter-label">统计时间范围</span>
        </div>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :shortcuts="shortcuts"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          :clearable="false"
        />
      </div>
    </el-card>

    <!-- Row 2: KPI Cards -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <div class="kpi-card kpi-card-blue">
          <div class="kpi-icon-wrapper">
            <el-icon class="kpi-icon"><Money /></el-icon>
          </div>
          <div class="kpi-content">
            <div class="kpi-title">营业收入</div>
            <div class="kpi-value">
              <span class="currency">¥</span>
              <span class="number">{{ formatNumber(kpiData.totalAmount) }}</span>
            </div>
            <div class="kpi-extra">
              <el-icon><ShoppingCart /></el-icon>
              <span>{{ kpiData.orderCount }} 笔订单</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-card-green">
          <div class="kpi-icon-wrapper">
            <el-icon class="kpi-icon"><TrendCharts /></el-icon>
          </div>
          <div class="kpi-content">
            <div class="kpi-title">盈利情况</div>
            <div class="kpi-value">
              <span class="currency">¥</span>
              <span class="number">{{ formatNumber(kpiData.totalProfit) }}</span>
            </div>
            <div class="kpi-extra">
              <el-icon><DataAnalysis /></el-icon>
              <span>毛利率 {{ formatRate(kpiData.profitRate) }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-card-orange">
          <div class="kpi-icon-wrapper">
            <el-icon class="kpi-icon"><User /></el-icon>
          </div>
          <div class="kpi-content">
            <div class="kpi-title">客单分析</div>
            <div class="kpi-value">
              <span class="currency">¥</span>
              <span class="number">{{ formatNumber(kpiData.customerPrice) }}</span>
            </div>
            <div class="kpi-extra">
              <el-icon><Wallet /></el-icon>
              <span>平均每单消费</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-card-purple">
          <div class="kpi-icon-wrapper">
            <el-icon class="kpi-icon"><Goods /></el-icon>
          </div>
          <div class="kpi-content">
            <div class="kpi-title">动销情况</div>
            <div class="kpi-value">
              <span class="number">{{ kpiData.totalQuantity }}</span>
              <span class="unit">件</span>
            </div>
            <div class="kpi-extra">
              <el-icon><Box /></el-icon>
              <span>累计销售商品</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Row 3: Trend Chart -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon class="header-icon"><DataLine /></el-icon>
            <span>营业额与毛利趋势</span>
          </div>
        </div>
      </template>
      <div ref="trendChartRef" class="chart-container" style="height: 350px;"></div>
    </el-card>

    <!-- Row 4: Bottom Charts -->
    <el-row :gutter="20" class="bottom-row">
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <el-icon class="header-icon" style="color: #409EFF;"><Trophy /></el-icon>
                <span>商品销量 TOP 10</span>
              </div>
            </div>
          </template>
          <div ref="topChartRef" class="chart-container" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <el-icon class="header-icon" style="color: #67C23A;"><PieChart /></el-icon>
                <span>品类销售占比</span>
              </div>
            </div>
          </template>
          <div ref="categoryChartRef" class="chart-container" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <el-icon class="header-icon" style="color: #E6A23C;"><CreditCard /></el-icon>
                <span>支付方式偏好</span>
              </div>
            </div>
          </template>
          <div ref="paymentChartRef" class="chart-container" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../../utils/request'
import { ElMessage } from 'element-plus'
import {
  Calendar, Money, TrendCharts, User, Goods, ShoppingCart,
  DataAnalysis, Wallet, Box, DataLine, Trophy, PieChart, CreditCard
} from '@element-plus/icons-vue'

// --- Date Picker Logic ---
const dateRange = ref([])
const shortcuts = [
  {
    text: '今日',
    value: () => {
      const end = new Date()
      const start = new Date()
      return [start, end]
    },
  },
  {
    text: '昨日',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24)
      end.setTime(end.getTime() - 3600 * 1000 * 24)
      return [start, end]
    },
  },
  {
    text: '近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 6)
      return [start, end]
    },
  },
  {
    text: '近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 29)
      return [start, end]
    },
  },
  {
    text: '本月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(1)
      return [start, end]
    },
  },
]

// Initialize default date range (Last 30 days)
const initDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 3600 * 1000 * 24 * 29)

  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  dateRange.value = [formatDate(start), formatDate(end)]
}

// --- KPI Data ---
const kpiData = reactive({
  totalAmount: 0,
  totalProfit: 0,
  orderCount: 0,
  totalQuantity: 0,
  customerPrice: 0,
  profitRate: 0
})

// --- Charts Refs & Instances ---
const trendChartRef = ref(null)
const topChartRef = ref(null)
const categoryChartRef = ref(null)
const paymentChartRef = ref(null)

let trendChart = null
let topChart = null
let categoryChart = null
let paymentChart = null

// --- Fetch Data ---
const fetchData = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }

  const [startDate, endDate] = dateRange.value
  const params = { startDate, endDate }

  try {
    const [kpiRes, trendRes, topRes, catRes, payRes] = await Promise.all([
      request.get('/sales-analysis/kpi', { params }),
      request.get('/sales-analysis/trend', { params }),
      request.get('/sales-analysis/top-products', { params }),
      request.get('/sales-analysis/category-pie', { params }),
      request.get('/sales-analysis/payment-pie', { params })
    ])

    if (kpiRes.data) {
      Object.assign(kpiData, kpiRes.data)
    }

    renderTrendChart(trendRes.data)
    renderTopChart(topRes.data)
    renderCategoryChart(catRes.data)
    renderPaymentChart(payRes.data)

  } catch (error) {
    console.error('Failed to fetch sales analysis data', error)
  }
}

// --- Chart Render Functions ---

const renderTrendChart = (data) => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross', label: { backgroundColor: '#6a7985' } },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: {
      data: ['销售额', '毛利润'],
      top: 10,
      textStyle: { color: '#666' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 50, containLabel: true },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: data.dates || [],
        axisLine: { lineStyle: { color: '#ddd' } },
        axisLabel: { color: '#666' }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        position: 'left',
        axisLabel: { formatter: '{value}', color: '#666' },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '毛利润',
        position: 'right',
        axisLabel: { formatter: '{value}', color: '#666' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        yAxisIndex: 0,
        symbol: 'circle',
        symbolSize: 6,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.4)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        lineStyle: { width: 3, color: '#409EFF' },
        itemStyle: { color: '#409EFF' },
        data: data.amounts || []
      },
      {
        name: '毛利润',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#67C23A' },
        itemStyle: { color: '#67C23A' },
        data: data.profits || []
      }
    ]
  }
  trendChart.setOption(option)
}

const renderTopChart = (data) => {
  if (!topChartRef.value) return
  if (!topChart) topChart = echarts.init(topChartRef.value)

  const sortedData = [...(data || [])].reverse()
  const categories = sortedData.map(item => item.productName)
  const values = sortedData.map(item => item.quantity)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    grid: { left: '3%', right: '10%', bottom: '3%', top: 10, containLabel: true },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01],
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666', width: 100, overflow: 'truncate' }
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        data: values,
        barWidth: 16,
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#79bbff' }
          ])
        }
      }
    ]
  }
  topChart.setOption(option)
}

const renderCategoryChart = (data) => {
  if (!categoryChartRef.value) return
  if (!categoryChart) categoryChart = echarts.init(categoryChartRef.value)

  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#b37beb', '#ff85c0', '#36cfc9']

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b} : ¥{c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: {
      top: 'bottom',
      textStyle: { color: '#666' }
    },
    color: colors,
    series: [
      {
        name: '品类占比',
        type: 'pie',
        radius: [30, 80],
        center: ['50%', '40%'],
        roseType: 'area',
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: {
          show: true,
          color: '#666',
          fontSize: 12,
          formatter: '{b}\n{d}%'
        },
        labelLine: {
          show: true,
          length: 8,
          length2: 12
        },
        data: data || []
      }
    ]
  }
  categoryChart.setOption(option)
}

const renderPaymentChart = (data) => {
  if (!paymentChartRef.value) return
  if (!paymentChart) paymentChart = echarts.init(paymentChartRef.value)

  const colors = ['#67C23A', '#409EFF', '#E6A23C']

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: {
      top: '5%',
      left: 'center',
      textStyle: { color: '#666' }
    },
    color: colors,
    series: [
      {
        name: '支付方式',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '52%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold',
            color: '#333'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          }
        },
        labelLine: { show: false },
        data: data || []
      }
    ]
  }
  paymentChart.setOption(option)
}

// --- Utils ---
const formatNumber = (num) => {
  if (num === undefined || num === null) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatRate = (rate) => {
  if (rate === undefined || rate === null) return '0.00%'
  return (Number(rate) * 100).toFixed(2) + '%'
}

const handleDateChange = () => {
  fetchData()
}

const resizeHandler = () => {
  trendChart?.resize()
  topChart?.resize()
  categoryChart?.resize()
  paymentChart?.resize()
}

// --- Lifecycle ---
onMounted(() => {
  initDateRange()
  nextTick(() => {
    fetchData()
    window.addEventListener('resize', resizeHandler)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
  trendChart?.dispose()
  topChart?.dispose()
  categoryChart?.dispose()
  paymentChart?.dispose()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

/* Filter Card */
.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-left {
  display: flex;
  align-items: center;
}

.filter-icon {
  font-size: 20px;
  color: #409EFF;
  margin-right: 10px;
}

.filter-label {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

/* KPI Cards */
.kpi-row {
  margin-bottom: 20px;
}

.kpi-card {
  border-radius: 16px;
  padding: 24px;
  height: 140px;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

.kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  opacity: 0.1;
  transform: translate(30%, -30%);
}

/* Blue Theme */
.kpi-card-blue {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
}
.kpi-card-blue::before { background: #fff; }

/* Green Theme */
.kpi-card-green {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: #fff;
}
.kpi-card-green::before { background: #fff; }

/* Orange Theme */
.kpi-card-orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: #fff;
}
.kpi-card-orange::before { background: #fff; }

/* Purple Theme */
.kpi-card-purple {
  background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%);
  color: #fff;
}
.kpi-card-purple::before { background: #fff; }

.kpi-icon-wrapper {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  flex-shrink: 0;
}

.kpi-icon {
  font-size: 28px;
  color: #fff;
}

.kpi-content {
  flex: 1;
}

.kpi-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
  display: flex;
  align-items: baseline;
}

.kpi-value .currency {
  font-size: 16px;
  margin-right: 4px;
  opacity: 0.9;
}

.kpi-value .number {
  font-size: 28px;
}

.kpi-value .unit {
  font-size: 14px;
  margin-left: 4px;
  opacity: 0.9;
}

.kpi-extra {
  font-size: 13px;
  opacity: 0.85;
  display: flex;
  align-items: center;
  gap: 4px;
}

.kpi-extra .el-icon {
  font-size: 14px;
}

/* Chart Cards */
.chart-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.chart-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-left {
  display: flex;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.header-icon {
  font-size: 20px;
  margin-right: 10px;
  color: #409EFF;
}

.chart-container {
  padding: 10px;
}

.bottom-row {
  margin-bottom: 20px;
}

/* Responsive */
@media (max-width: 1200px) {
  .kpi-card {
    padding: 20px;
    height: auto;
  }

  .kpi-value .number {
    font-size: 24px;
  }
}

@media (max-width: 768px) {
  .kpi-row .el-col {
    margin-bottom: 15px;
  }

  .bottom-row .el-col {
    margin-bottom: 15px;
  }
}
</style>