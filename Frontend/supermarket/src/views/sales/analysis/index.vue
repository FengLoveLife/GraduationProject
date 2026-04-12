<template>
  <div class="app-container">
    <!-- Row 1: Page Header with Date Picker -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">销售统计分析</h2>
        <span class="page-subtitle">多维度洞察经营状况，数据驱动决策</span>
      </div>
      <div class="header-right">
        <div class="date-picker-wrapper">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :shortcuts="shortcuts"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
            :clearable="false"
            size="large"
          />
        </div>
      </div>
    </div>

    <!-- Row 2: KPI Cards -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-decorator kpi-deco-blue"></div>
        <div class="kpi-body">
          <div class="kpi-header">
            <span class="kpi-title">营业收入</span>
            <div class="kpi-icon-circle kpi-icon-blue">
              <el-icon><Money /></el-icon>
            </div>
          </div>
          <div class="kpi-value">
            <span class="currency">¥</span>
            <span class="number">{{ formatCompact(kpiData.totalAmount) }}</span>
          </div>
          <div class="kpi-footer">
            <span class="kpi-tag kpi-tag-blue">{{ kpiData.orderCount }} 笔订单</span>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-decorator kpi-deco-green"></div>
        <div class="kpi-body">
          <div class="kpi-header">
            <span class="kpi-title">盈利情况</span>
            <div class="kpi-icon-circle kpi-icon-green">
              <el-icon><TrendCharts /></el-icon>
            </div>
          </div>
          <div class="kpi-value">
            <span class="currency">¥</span>
            <span class="number">{{ formatCompact(kpiData.totalProfit) }}</span>
          </div>
          <div class="kpi-footer">
            <span class="kpi-tag kpi-tag-green">毛利率 {{ formatRate(kpiData.profitRate) }}</span>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-decorator kpi-deco-orange"></div>
        <div class="kpi-body">
          <div class="kpi-header">
            <span class="kpi-title">客单价</span>
            <div class="kpi-icon-circle kpi-icon-orange">
              <el-icon><Wallet /></el-icon>
            </div>
          </div>
          <div class="kpi-value">
            <span class="currency">¥</span>
            <span class="number">{{ formatNumber(kpiData.customerPrice) }}</span>
          </div>
          <div class="kpi-footer">
            <span class="kpi-tag kpi-tag-orange">平均每单消费</span>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-decorator kpi-deco-purple"></div>
        <div class="kpi-body">
          <div class="kpi-header">
            <span class="kpi-title">商品动销</span>
            <div class="kpi-icon-circle kpi-icon-purple">
              <el-icon><Goods /></el-icon>
            </div>
          </div>
          <div class="kpi-value">
            <span class="number">{{ formatQuantity(kpiData.totalQuantity) }}</span>
            <span class="unit">件</span>
          </div>
          <div class="kpi-footer">
            <span class="kpi-tag kpi-tag-purple">累计售出商品</span>
          </div>
        </div>
      </div>
    </div>

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
  Money, TrendCharts, Goods, Wallet,
  DataLine, Trophy, PieChart, CreditCard
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

/**
 * 智能缩写金额：超过万元显示 "X.XX万"，否则正常显示
 * 解决大金额数字过长溢出卡片的问题
 */
const formatCompact = (num) => {
  if (num === undefined || num === null) return '0.00'
  const n = Number(num)
  if (n >= 10000) {
    return (n / 10000).toFixed(2) + '万'
  }
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatQuantity = (num) => {
  if (num === undefined || num === null) return '0'
  const n = Number(num)
  if (n >= 10000) {
    return (n / 10000).toFixed(1) + '万'
  }
  return n.toLocaleString('zh-CN')
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

/* ==================== Page Header ==================== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 28px;
  background: linear-gradient(135deg, #1d3a8a 0%, #2755c5 50%, #3b7dd8 100%);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(29, 58, 138, 0.25);
  position: relative;
  overflow: hidden;
}

/* 装饰圆 */
.page-header::before {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.04);
  top: -80px;
  right: 60px;
}

.page-header::after {
  content: '';
  position: absolute;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  bottom: -50px;
  right: 200px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
  position: relative;
  z-index: 1;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.page-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.header-right {
  position: relative;
  z-index: 1;
}

.date-picker-wrapper {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  padding: 4px;
  backdrop-filter: blur(8px);
}

.date-picker-wrapper :deep(.el-date-editor) {
  --el-date-editor-width: 340px;
}

.date-picker-wrapper :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: none;
  border: none;
}

/* ==================== KPI Cards Grid ==================== */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.kpi-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

/* 左侧彩色装饰条 */
.kpi-decorator {
  width: 5px;
  flex-shrink: 0;
}

.kpi-deco-blue   { background: linear-gradient(180deg, #409EFF, #79bbff); }
.kpi-deco-green  { background: linear-gradient(180deg, #67C23A, #95d475); }
.kpi-deco-orange { background: linear-gradient(180deg, #E6A23C, #f0c78a); }
.kpi-deco-purple { background: linear-gradient(180deg, #8B5CF6, #b794f6); }

.kpi-body {
  flex: 1;
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 130px;
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kpi-title {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

/* 右上角圆形图标 */
.kpi-icon-circle {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.kpi-icon-blue   { background: #ecf5ff; color: #409EFF; }
.kpi-icon-green  { background: #f0f9eb; color: #67C23A; }
.kpi-icon-orange { background: #fdf6ec; color: #E6A23C; }
.kpi-icon-purple { background: #f3eeff; color: #8B5CF6; }

/* 数值区域 */
.kpi-value {
  display: flex;
  align-items: baseline;
  margin: 10px 0;
}

.kpi-value .currency {
  font-size: 16px;
  font-weight: 600;
  color: #909399;
  margin-right: 2px;
}

.kpi-value .number {
  font-size: 28px;
  font-weight: 700;
  color: #1d2129;
  letter-spacing: -0.5px;
  line-height: 1;
}

.kpi-value .unit {
  font-size: 14px;
  color: #909399;
  margin-left: 4px;
  font-weight: 500;
}

/* 底部标签 */
.kpi-footer {
  display: flex;
  align-items: center;
}

.kpi-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 500;
}

.kpi-tag-blue   { background: #ecf5ff; color: #409EFF; }
.kpi-tag-green  { background: #f0f9eb; color: #67C23A; }
.kpi-tag-orange { background: #fdf6ec; color: #E6A23C; }
.kpi-tag-purple { background: #f3eeff; color: #8B5CF6; }

/* ==================== Chart Cards ==================== */
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

/* ==================== Responsive ==================== */
@media (max-width: 1400px) {
  .kpi-value .number {
    font-size: 24px;
  }
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .bottom-row .el-col {
    margin-bottom: 15px;
  }
}
</style>