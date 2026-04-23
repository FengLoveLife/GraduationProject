<template>
  <div class="forecast-dashboard" v-loading="loading">
    <!-- 头部标题 -->
    <div class="header-section">
      <div class="header-left">
        <h2 class="title">智能销量预测中心</h2>
        <p class="subtitle">基于机器学习模型，精准预测商品销量趋势</p>
      </div>
    </div>

    <!-- KPI 卡片区 -->
    <el-row :gutter="20" class="kpi-section">
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-blue">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">今日预测商品</div>
              <div class="kpi-value">{{ kpiData.totalProducts }}<span class="unit">个</span></div>
              <div class="kpi-trend-placeholder"></div>
            </div>
            <div class="icon-wrapper"><el-icon><Goods /></el-icon></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-green">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">预测总销量</div>
              <div class="kpi-value">{{ kpiData.totalPredicted }}<span class="unit">件</span></div>
              <div v-if="formatGrowthRate(kpiData.quantityGrowthRate)" class="kpi-trend" :class="formatGrowthRate(kpiData.quantityGrowthRate).type">
                <el-icon><TrendCharts /></el-icon>
                {{ formatGrowthRate(kpiData.quantityGrowthRate).text }}
              </div>
            </div>
            <div class="icon-wrapper"><el-icon><TrendCharts /></el-icon></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-orange">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">预测总销售额</div>
              <div class="kpi-value">¥{{ formatAmount(kpiData.totalPredictedAmount) }}</div>
              <div v-if="formatGrowthRate(kpiData.amountGrowthRate)" class="kpi-trend" :class="formatGrowthRate(kpiData.amountGrowthRate).type">
                <el-icon><Money /></el-icon>
                {{ formatGrowthRate(kpiData.amountGrowthRate).text }}
              </div>
            </div>
            <div class="icon-wrapper"><el-icon><Money /></el-icon></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第一行图表 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><DataLine /></el-icon>
                <span>预测销售额趋势 vs 实际销售额</span>
              </div>
              <div class="header-right">
                <el-radio-group v-model="trendRange" size="small">
                  <el-radio-button label="7">近 7 天</el-radio-button>
                  <el-radio-button label="14">近 14 天</el-radio-button>
                  <el-radio-button label="30">近 30 天</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" style="color: #E6A23C"><PieChart /></el-icon>
                <span>各分类预测销量占比</span>
              </div>
            </div>
          </template>
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 热销 TOP10 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" style="color: #F56C6C"><TrendCharts /></el-icon>
                <span>热销商品 TOP10 预测</span>
              </div>
              <el-button type="primary" link @click="goToPredict">查看全部</el-button>
            </div>
          </template>
          <div class="top-product-list">
            <div v-for="(item, index) in topProducts" :key="item.productId" class="top-product-item">
              <div class="rank" :class="getRankClass(index)">{{ index + 1 }}</div>
              <div class="product-info">
                <div class="product-name">{{ item.productName }}</div>
                <div class="product-category">{{ item.categoryName }}</div>
              </div>
              <div class="predict-value">
                <span class="value">{{ item.predictedQuantity }}</span>
                <span class="unit">件</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 未来7日预测日销量分布 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card week-forecast-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" style="color: #8B5CF6"><DataLine /></el-icon>
                <span>未来7日预测日销量</span>
              </div>
              <div class="week-legend">
                <span class="legend-item">
                  <span class="legend-dot" style="background: #8B5CF6;"></span>工作日
                </span>
                <span class="legend-item">
                  <span class="legend-dot" style="background: #F59E0B;"></span>周末
                </span>
                <el-button type="primary" link @click="goToPredict">查看明细</el-button>
              </div>
            </div>
          </template>

          <!-- 三项汇总统计 -->
          <div class="week-stats-bar">
            <div class="week-stat-item">
              <div class="week-stat-label">7日预测总量</div>
              <div class="week-stat-value">
                {{ weekForecastStats.total.toLocaleString() }}<span class="unit">件</span>
              </div>
            </div>
            <div class="week-stat-divider"></div>
            <div class="week-stat-item">
              <div class="week-stat-label">日均预测</div>
              <div class="week-stat-value">
                {{ weekForecastStats.avg }}<span class="unit">件/天</span>
              </div>
            </div>
            <div class="week-stat-divider"></div>
            <div class="week-stat-item">
              <div class="week-stat-label">预测峰值日</div>
              <div class="week-stat-value peak">{{ weekForecastStats.peakDay || '—' }}</div>
            </div>
          </div>

          <div ref="weekForecastChartRef" class="week-chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 未来7日预测销售额与毛利 -->
    <el-card shadow="hover" class="revenue-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon" style="color: #409EFF"><DataLine /></el-icon>
            <span>未来7日预测销售额与毛利</span>
          </div>
          <div class="revenue-legend">
            <span class="legend-item"><span class="legend-dot" style="background:#409EFF;"></span>销售额·工作日</span>
            <span class="legend-item"><span class="legend-dot" style="background:#F59E0B;"></span>销售额·周末</span>
            <span class="legend-item"><span class="legend-dot" style="background:#67C23A;"></span>毛利·工作日</span>
            <span class="legend-item"><span class="legend-dot" style="background:#10B981;"></span>毛利·周末</span>
          </div>
        </div>
      </template>

      <!-- 汇总统计栏 -->
      <div class="revenue-stats-bar">
        <div class="revenue-stat-item">
          <div class="revenue-stat-label">7日预测总销售额</div>
          <div class="revenue-stat-value blue">¥{{ formatAmount(weekRevenueStats.totalRevenue) }}</div>
        </div>
        <div class="revenue-stat-divider"></div>
        <div class="revenue-stat-item">
          <div class="revenue-stat-label">7日预测总毛利</div>
          <div class="revenue-stat-value green">¥{{ formatAmount(weekRevenueStats.totalProfit) }}</div>
        </div>
        <div class="revenue-stat-divider"></div>
        <div class="revenue-stat-item">
          <div class="revenue-stat-label">销售额峰值日</div>
          <div class="revenue-stat-value peak">{{ weekRevenueStats.peakDay || '—' }}</div>
        </div>
      </div>

      <div ref="revenueChartRef" class="revenue-chart-box"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Goods, TrendCharts, DataLine, PieChart, Money } from '@element-plus/icons-vue'
import { getForecastResults, getForecastSummary, getForecastTrendDaily } from '@/api/forecast'

const router = useRouter()
const loading = ref(false)
const trendRange = ref('7')

const kpiData = reactive({
  totalProducts: 0, totalPredicted: 0, totalPredictedAmount: 0,
  quantityGrowthRate: null, amountGrowthRate: null
})
const categoryAmountsData = ref([])

// 图表 DOM 引用
const trendChartRef = ref(null)
const statusChartRef = ref(null)
const weekForecastChartRef = ref(null)
const revenueChartRef = ref(null)
let trendChart = null
let statusChart = null
let weekForecastChart = null
let revenueChart = null

// 数据
const topProducts = ref([])
const allForecastResults = ref([])
const weekForecastData = ref([])
const weekForecastStats = reactive({ total: 0, avg: 0, peakDay: '' })
const weekRevenueData = ref([])
const weekRevenueStats = reactive({ totalRevenue: 0, totalProfit: 0, peakDay: '' })

// ===================== 工具函数 =====================

const getRankClass = (i) => ['rank-1', 'rank-2', 'rank-3'][i] || ''

const formatAmount = (amount) =>
  amount >= 10000 ? (amount / 10000).toFixed(2) + '万' : Number(amount || 0).toFixed(2)

const formatGrowthRate = (rate) => {
  if (rate === null || rate === undefined) return null
  const num = parseFloat(rate)
  if (num > 0) return { text: `较昨日 +${num}%`, type: 'up' }
  if (num < 0) return { text: `较昨日 ${num}%`, type: 'down' }
  return { text: '较昨日持平', type: 'flat' }
}

const formatDate = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const goToPredict = () => router.push('/forecasting/predict')

// ===================== 数据加载 =====================

const loadKpiData = async () => {
  try {
    const res = await getForecastSummary()
    if (res.code === 200 && res.data) {
      const data = res.data
      kpiData.totalProducts       = data.totalProducts || 0
      kpiData.totalPredicted      = data.totalPredicted || 0
      kpiData.totalPredictedAmount = data.totalPredictedAmount || 0
      kpiData.quantityGrowthRate  = data.quantityGrowthRate
      kpiData.amountGrowthRate    = data.amountGrowthRate
      const catAmounts = data.categoryAmounts || {}
      categoryAmountsData.value = Object.entries(catAmounts)
        .map(([name, value]) => ({ name, value: Number(value) || 0 }))
        .filter(d => d.value > 0)
        .sort((a, b) => b.value - a.value)
    }
  } catch (e) { console.error('KPI 加载失败:', e) }
}

const loadForecastResults = async () => {
  try {
    // 一次加载7天数据，兼顾 TOP10 和日销量分布图
    const res = await getForecastResults({ days: 7 })
    if (res.code !== 200 || !res.data) return

    const results = res.data
    allForecastResults.value = results

    // ---- TOP10：取第一天数据按预测量降序 ----
    const dates = [...new Set(results.map(r => r.forecastDate))].sort()
    const firstDate = dates[0]
    const firstDay  = firstDate ? results.filter(r => r.forecastDate === firstDate) : results
    topProducts.value = [...firstDay]
      .sort((a, b) => b.predictedQuantity - a.predictedQuantity)
      .slice(0, 10)

    // ---- 7日日销量分布：按日期聚合 ----
    const dayMap = new Map()
    for (const r of results) {
      dayMap.set(r.forecastDate, (dayMap.get(r.forecastDate) || 0) + (r.predictedQuantity || 0))
    }

    const DAY_NAMES = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    weekForecastData.value = dates.map(dateStr => {
      const d   = new Date(dateStr + 'T00:00:00')
      const dow = d.getDay()
      const [, mm, dd] = dateStr.split('-')
      return {
        date:           dateStr,
        dayLabel:       `${parseInt(mm)}/${parseInt(dd)}\n${DAY_NAMES[dow]}`,
        shortLabel:     `${parseInt(mm)}/${parseInt(dd)} ${DAY_NAMES[dow]}`,
        isWeekend:      dow === 0 || dow === 6,
        totalPredicted: dayMap.get(dateStr) || 0
      }
    })

    // 统计
    const totals = weekForecastData.value.map(d => d.totalPredicted)
    if (totals.length > 0) {
      const total   = totals.reduce((a, b) => a + b, 0)
      const peakIdx = totals.indexOf(Math.max(...totals))
      weekForecastStats.total   = total
      weekForecastStats.avg     = Math.round(total / totals.length)
      weekForecastStats.peakDay = weekForecastData.value[peakIdx]?.shortLabel || ''
    }

    // ---- 7日销售额与毛利分布 ----
    const revenueByDate = new Map()
    for (const r of results) {
      if (!r.forecastDate) continue
      const selling  = Number(r.sellingPrice  || 0)
      const purchase = Number(r.purchasePrice || 0)
      const qty      = r.predictedQuantity    || 0
      const existing = revenueByDate.get(r.forecastDate) || { revenue: 0, profit: 0 }
      revenueByDate.set(r.forecastDate, {
        revenue: existing.revenue + qty * selling,
        profit:  existing.profit  + qty * (selling - purchase)
      })
    }
    weekRevenueData.value = dates.map(dateStr => {
      const d   = new Date(dateStr + 'T00:00:00')
      const dow = d.getDay()
      const [, mm, dd] = dateStr.split('-')
      const entry = revenueByDate.get(dateStr) || { revenue: 0, profit: 0 }
      return {
        dayLabel:  `${parseInt(mm)}/${parseInt(dd)}\n${DAY_NAMES[dow]}`,
        shortLabel:`${parseInt(mm)}/${parseInt(dd)} ${DAY_NAMES[dow]}`,
        isWeekend: dow === 0 || dow === 6,
        revenue:   Number(entry.revenue.toFixed(2)),
        profit:    Number(entry.profit.toFixed(2))
      }
    })

    // 汇总统计
    if (weekRevenueData.value.length > 0) {
      const totalRevenue = weekRevenueData.value.reduce((s, d) => s + d.revenue, 0)
      const totalProfit  = weekRevenueData.value.reduce((s, d) => s + d.profit,  0)
      const peakIdx      = weekRevenueData.value.reduce((maxI, d, i, arr) => d.revenue > arr[maxI].revenue ? i : maxI, 0)
      weekRevenueStats.totalRevenue = totalRevenue
      weekRevenueStats.totalProfit  = totalProfit
      weekRevenueStats.peakDay      = weekRevenueData.value[peakIdx]?.shortLabel || ''
    }
  } catch (e) { console.error('预测结果加载失败:', e) }
}

// ===================== 图表初始化 =====================

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  try {
    const days = parseInt(trendRange.value)
    const end   = new Date()
    const start = new Date()
    start.setDate(start.getDate() - days + 1)

    const res = await getForecastTrendDaily({ startDate: formatDate(start), endDate: formatDate(end) })
    const trendData = res.data || []
    const dateLabels = trendData.map(item => item.date.substring(5))
    const predicted  = trendData.map(item => Number(Number(item.predictedAmount || 0).toFixed(2)))
    const actual     = trendData.map(item => Number(Number(item.actualAmount    || 0).toFixed(2)))

    const fmtY = (v) => v >= 10000 ? (v / 10000).toFixed(1) + '万' : v
    const fmtM = (v) => {
      const n = Number(v) || 0
      return n >= 10000 ? '¥' + (n / 10000).toFixed(2) + '万' : '¥' + n.toFixed(2)
    }

    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: '#e4e7ed',
        textStyle: { color: '#303133' },
        formatter: (params) => {
          if (!params?.length) return ''
          let html = `<div style="font-weight:600;margin-bottom:4px;">${params[0].axisValue}</div>`
          params.forEach(p => {
            html += `<div style="display:flex;align-items:center;margin:2px 0;">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color};margin-right:8px;"></span>
              <span style="flex:1;">${p.seriesName}</span>
              <span style="font-weight:600;margin-left:16px;">${fmtM(p.value)}</span>
            </div>`
          })
          return html
        }
      },
      legend: { data: ['实际销售额', '预测销售额'], top: 10, textStyle: { color: '#606266' } },
      grid:   { left: '3%', right: '4%', bottom: '3%', top: 60, containLabel: true },
      xAxis:  { type: 'category', boundaryGap: false, data: dateLabels, axisLine: { lineStyle: { color: '#e4e7ed' } }, axisLabel: { color: '#909399' } },
      yAxis:  { type: 'value', axisLine: { show: false }, axisLabel: { color: '#909399', formatter: fmtY }, splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } } },
      series: [
        { name: '实际销售额', type: 'line', smooth: true, symbol: 'circle',  symbolSize: 8, data: actual,    lineStyle: { color: '#409EFF', width: 3 },               itemStyle: { color: '#409EFF' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.05)' }]) } },
        { name: '预测销售额', type: 'line', smooth: true, symbol: 'diamond', symbolSize: 8, data: predicted, lineStyle: { color: '#67C23A', width: 3, type: 'dashed' }, itemStyle: { color: '#67C23A' } }
      ]
    })
  } catch (e) {
    console.error('趋势图加载失败:', e)
    ElMessage.warning('暂无实际销售数据，图表显示预测数据')
  }
}

const initStatusChart = () => {
  if (!statusChartRef.value) return
  if (!statusChart) statusChart = echarts.init(statusChartRef.value)

  const pieData     = categoryAmountsData.value
  const totalAmount = pieData.reduce((s, d) => s + d.value, 0)
  const colors = ['#5470c6', '#67C23A', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
  const totalLabel  = totalAmount >= 10000
    ? (totalAmount / 10000).toFixed(1) + '万'
    : totalAmount.toFixed(0)

  statusChart.setOption({
    color: colors,
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.97)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#333', fontSize: 13 },
      extraCssText: 'box-shadow: 0 4px 16px rgba(0,0,0,0.1); border-radius: 8px;',
      formatter: (p) =>
        `<div style="font-weight:600;margin-bottom:4px">${p.name}</div>` +
        `<div style="color:#666">预测销售额：<span style="color:#303133;font-weight:600">¥${Number(p.value).toFixed(2)}</span></div>` +
        `<div style="color:#666">占比：<span style="color:${p.color};font-weight:600">${p.percent}%</span></div>`
    },
    legend: {
      bottom: 4,
      left: 'center',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 12,
      textStyle: { color: '#666', fontSize: 11 },
      formatter: (name) => name.length > 4 ? name.slice(0, 4) + '…' : name
    },
    graphic: [
      {
        type: 'text', left: 'center', top: '32%',
        style: { text: '¥' + totalLabel, textAlign: 'center', fill: '#1d2129', fontSize: 16, fontWeight: 'bold' }
      },
      {
        type: 'text', left: 'center', top: '43%',
        style: { text: '预测总额', textAlign: 'center', fill: '#909399', fontSize: 11 }
      }
    ],
    series: [{
      name: '分类占比',
      type: 'pie',
      radius: ['42%', '66%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 7, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 7,
        itemStyle: { shadowBlur: 16, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.22)' },
        label: { show: false },
        labelLine: { show: false }
      },
      data: pieData
    }]
  })
}

const initWeekForecastChart = () => {
  if (!weekForecastChartRef.value || weekForecastData.value.length === 0) return
  if (!weekForecastChart) weekForecastChart = echarts.init(weekForecastChartRef.value)

  const data = weekForecastData.value

  const barItems = data.map(d => ({
    value: d.totalPredicted,
    itemStyle: {
      color: d.isWeekend
        ? new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#F59E0B' }, { offset: 1, color: '#FEF3C7' }
          ])
        : new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#8B5CF6' }, { offset: 1, color: '#EDE9FE' }
          ])
    }
  }))

  weekForecastChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' },
      formatter: (params) => {
        const p    = params[0]
        const item = data[p.dataIndex]
        const tag  = item.isWeekend
          ? '<span style="color:#F59E0B;font-size:11px;margin-left:4px;font-weight:600;">周末</span>'
          : ''
        return `<div style="font-weight:600;margin-bottom:4px;">${item.date}${tag}</div>
                <div>${p.marker} 预测总销量：<b style="font-size:14px;">${p.value.toLocaleString()}</b> 件</div>`
      }
    },
    grid: { left: '2%', right: '3%', bottom: '6%', top: '16%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.dayLabel),
      axisLine:  { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399', fontSize: 11, lineHeight: 18, interval: 0 },
      axisTick:  { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine:  { show: false },
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: barItems,
      barMaxWidth: 46,
      itemStyle: { borderRadius: [8, 8, 0, 0] },
      label: {
        show: true,
        position: 'top',
        color: '#475569',
        fontSize: 11,
        fontWeight: 600,
        formatter: (p) => p.value > 0 ? p.value.toLocaleString() : ''
      },
      markLine: {
        silent: true,
        symbol: ['none', 'none'],
        lineStyle: { color: '#94a3b8', type: 'dashed', width: 1.5 },
        data: [{ type: 'average' }],
        label: { show: false }
      }
    }]
  })
}

const initRevenueChart = () => {
  if (!revenueChartRef.value || weekRevenueData.value.length === 0) return
  if (!revenueChart) revenueChart = echarts.init(revenueChartRef.value)

  const data   = weekRevenueData.value
  const fmtAmt = (v) => v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toFixed(0)
  const fmtTip = (v) => {
    const n = Number(v) || 0
    return n >= 10000 ? '¥' + (n / 10000).toFixed(2) + '万' : '¥' + n.toFixed(2)
  }

  const mkRevColor = (isWeekend) => new echarts.graphic.LinearGradient(0, 0, 0, 1, isWeekend
    ? [{ offset: 0, color: '#F59E0B' }, { offset: 1, color: '#FEF3C7' }]
    : [{ offset: 0, color: '#409EFF' }, { offset: 1, color: '#C6E2FF' }])
  const mkProfColor = (isWeekend) => new echarts.graphic.LinearGradient(0, 0, 0, 1, isWeekend
    ? [{ offset: 0, color: '#10B981' }, { offset: 1, color: '#D1FAE5' }]
    : [{ offset: 0, color: '#67C23A' }, { offset: 1, color: '#E2F0D9' }])

  revenueChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' },
      formatter: (params) => {
        if (!params?.length) return ''
        const item = data[params[0].dataIndex]
        const tag  = item.isWeekend
          ? '<span style="color:#F59E0B;font-size:11px;margin-left:4px;font-weight:600;">周末</span>'
          : ''
        let html = `<div style="font-weight:600;margin-bottom:4px;">${item.shortLabel}${tag}</div>`
        params.forEach(p => {
          html += `<div style="display:flex;align-items:center;margin:2px 0;">
            <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color};margin-right:8px;"></span>
            <span style="flex:1;">${p.seriesName}</span>
            <span style="font-weight:600;margin-left:16px;">${fmtTip(p.value)}</span>
          </div>`
        })
        return html
      }
    },
    legend: { show: false },
    grid:   { left: '2%', right: '3%', bottom: '6%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.dayLabel),
      axisLine:  { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399', fontSize: 11, lineHeight: 18, interval: 0 },
      axisTick:  { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine:  { show: false },
      axisLabel: { color: '#909399', fontSize: 11, formatter: fmtAmt },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [
      {
        name: '预测销售额',
        type: 'bar',
        data: data.map(d => ({ value: d.revenue, itemStyle: { color: mkRevColor(d.isWeekend), borderRadius: [6, 6, 0, 0] } })),
        barMaxWidth: 36,
        label: { show: true, position: 'top', fontSize: 10, fontWeight: 600,
          color: '#409EFF',
          formatter: (p) => {
            const isWeekend = data[p.dataIndex]?.isWeekend
            return p.value > 0 ? (isWeekend ? `{we|${fmtAmt(p.value)}}` : `{wd|${fmtAmt(p.value)}}`) : ''
          },
          rich: { wd: { color: '#409EFF', fontSize: 10, fontWeight: 600 }, we: { color: '#F59E0B', fontSize: 10, fontWeight: 600 } }
        }
      },
      {
        name: '预测毛利',
        type: 'bar',
        data: data.map(d => ({ value: d.profit, itemStyle: { color: mkProfColor(d.isWeekend), borderRadius: [6, 6, 0, 0] } })),
        barMaxWidth: 36,
        label: { show: true, position: 'top', fontSize: 10, fontWeight: 600,
          formatter: (p) => {
            const isWeekend = data[p.dataIndex]?.isWeekend
            return p.value > 0 ? (isWeekend ? `{we|${fmtAmt(p.value)}}` : `{wd|${fmtAmt(p.value)}}`) : ''
          },
          rich: { wd: { color: '#67C23A', fontSize: 10, fontWeight: 600 }, we: { color: '#10B981', fontSize: 10, fontWeight: 600 } }
        }
      }
    ]
  })
}

const initCharts = async () => {
  await nextTick()
  await initTrendChart()
  initStatusChart()
  initWeekForecastChart()
  initRevenueChart()
}

const handleResize = () => {
  trendChart?.resize()
  statusChart?.resize()
  weekForecastChart?.resize()
  revenueChart?.resize()
}

watch(trendRange, () => initTrendChart())

const loadAllData = async () => {
  loading.value = true
  try {
    await Promise.all([loadKpiData(), loadForecastResults()])
    initCharts()
  } catch (e) {
    console.error('加载数据失败:', e)
    ElMessage.error('加载数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  statusChart?.dispose()
  weekForecastChart?.dispose()
  revenueChart?.dispose()
})
</script>

<style scoped lang="scss">
.forecast-dashboard { padding: 24px; background-color: #f8fafc; min-height: calc(100vh - 100px); }

.header-section {
  margin-bottom: 24px;
  display: flex; justify-content: space-between; align-items: flex-start;
  .title   { font-size: 24px; font-weight: 800; color: #1e293b; margin: 0; }
  .subtitle { font-size: 14px; color: #64748b; margin: 8px 0 0; }
}

.kpi-section {
  margin-bottom: 24px;
  .kpi-card {
    border-radius: 16px; border: none;
    box-shadow: 0 4px 20px rgba(0,0,0,0.04); transition: all 0.3s ease; overflow: hidden;
    :deep(.el-card__body) { padding: 20px; }
    .kpi-card-content { display: flex; justify-content: space-between; align-items: center;
      .kpi-text {
        .kpi-label { font-size: 14px; color: #64748b; margin-bottom: 8px; }
        .kpi-value { font-size: 32px; font-weight: 800; color: #1e293b; margin-bottom: 8px; .unit { font-size: 16px; font-weight: 400; color: #94a3b8; margin-left: 2px; } }
        .kpi-trend { font-size: 12px; display: flex; align-items: center; gap: 4px; &.up { color: #67C23A; } &.down { color: #F56C6C; } &.flat { color: #909399; } }
        .kpi-trend-placeholder { height: 20px; }
      }
      .icon-wrapper { width: 64px; height: 64px; border-radius: 16px; display: flex; justify-content: center; align-items: center; font-size: 28px; }
    }
    &.kpi-blue   { background: linear-gradient(135deg,#EBF5FF,#DBEAFE); .icon-wrapper { background: rgba(59,130,246,0.15); color: #3B82F6; } }
    &.kpi-green  { background: linear-gradient(135deg,#ECFDF5,#D1FAE5); .icon-wrapper { background: rgba(16,185,129,0.15); color: #10B981; } }
    &.kpi-orange { background: linear-gradient(135deg,#FFF7ED,#FFEDD5); .icon-wrapper { background: rgba(245,158,11,0.15);  color: #F59E0B; } }
  }
}

.chart-row { margin-bottom: 20px; }

.chart-card {
  border-radius: 16px; border: none;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  :deep(.el-card__header) { border-bottom: 1px solid #f1f5f9; padding: 16px 20px; }
  .card-header {
    display: flex; justify-content: space-between; align-items: center;
    .header-left { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 700; color: #334155;
      .header-icon { font-size: 20px; color: #409EFF; }
    }
  }
  .chart-box { height: 300px; width: 100%; }
}

// -------- 热销 TOP10 --------
.top-product-list {
  max-height: 320px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 2px; }
  &::-webkit-scrollbar-thumb:hover { background: #94a3b8; }
  .top-product-item {
    display: flex; align-items: center; padding: 12px 16px; border-radius: 10px;
    transition: all 0.2s ease; margin-bottom: 8px; background: #f8fafc;
    &:hover { background: #f1f5f9; transform: translateX(4px); }
    .rank {
      width: 28px; height: 28px; border-radius: 8px; display: flex;
      justify-content: center; align-items: center; font-size: 14px; font-weight: 700;
      background: #e2e8f0; color: #64748b; margin-right: 12px; flex-shrink: 0;
      &.rank-1 { background: linear-gradient(135deg,#F56C6C,#E6A23C); color: #fff; }
      &.rank-2 { background: linear-gradient(135deg,#909399,#C0C4CC); color: #fff; }
      &.rank-3 { background: linear-gradient(135deg,#E6A23C,#F5D442); color: #fff; }
    }
    .product-info {
      flex: 1; min-width: 0;
      .product-name     { font-size: 14px; font-weight: 600; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .product-category { font-size: 12px; color: #94a3b8; margin-top: 2px; }
    }
    .predict-value {
      text-align: right;
      .value { font-size: 18px; font-weight: 700; color: #F56C6C; }
      .unit  { font-size: 12px; color: #94a3b8; margin-left: 2px; }
    }
  }
}

// -------- 未来7日预测卡片 --------
.week-forecast-card {
  .card-header {
    .week-legend {
      display: flex; align-items: center; gap: 12px;
      .legend-item {
        display: flex; align-items: center; gap: 5px;
        font-size: 12px; color: #64748b;
        .legend-dot { display: inline-block; width: 10px; height: 10px; border-radius: 3px; flex-shrink: 0; }
      }
    }
  }

  .week-stats-bar {
    display: flex; align-items: center;
    padding: 12px 16px; background: #f8fafc; border-radius: 10px; margin-bottom: 14px;

    .week-stat-item {
      flex: 1; text-align: center;
      .week-stat-label { font-size: 12px; color: #94a3b8; margin-bottom: 4px; }
      .week-stat-value {
        font-size: 20px; font-weight: 700; color: #1e293b;
        .unit { font-size: 11px; color: #94a3b8; margin-left: 2px; font-weight: 400; }
        &.peak { font-size: 15px; color: #F59E0B; }
      }
    }

    .week-stat-divider { width: 1px; height: 32px; background: #e2e8f0; margin: 0 8px; }
  }

  .week-chart-box { height: 246px; width: 100%; }
}

// -------- 销售额与毛利图 --------
.revenue-card {
  border-radius: 16px; border: none; box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  :deep(.el-card__header) { border-bottom: 1px solid #f1f5f9; padding: 16px 20px; }

  .card-header {
    display: flex; justify-content: space-between; align-items: center;
    .header-left { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 700; color: #334155;
      .header-icon { font-size: 20px; }
    }
  }

  .revenue-legend {
    display: flex; align-items: center; gap: 12px;
    .legend-item {
      display: flex; align-items: center; gap: 5px;
      font-size: 12px; color: #64748b;
      .legend-dot { display: inline-block; width: 10px; height: 10px; border-radius: 3px; flex-shrink: 0; }
    }
  }

  .revenue-stats-bar {
    display: flex; align-items: center;
    padding: 12px 16px; background: #f8fafc; border-radius: 10px; margin-bottom: 14px;
    .revenue-stat-item {
      flex: 1; text-align: center;
      .revenue-stat-label { font-size: 12px; color: #94a3b8; margin-bottom: 4px; }
      .revenue-stat-value {
        font-size: 20px; font-weight: 700; color: #1e293b;
        &.blue  { color: #409EFF; }
        &.green { color: #67C23A; }
        &.peak  { font-size: 15px; color: #F59E0B; }
      }
    }
    .revenue-stat-divider { width: 1px; height: 32px; background: #e2e8f0; margin: 0 8px; }
  }

  .revenue-chart-box { height: 200px; width: 100%; }
}
</style>
