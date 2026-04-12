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
            <div class="icon-wrapper">
              <el-icon><Goods /></el-icon>
            </div>
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
            <div class="icon-wrapper">
              <el-icon><TrendCharts /></el-icon>
            </div>
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
            <div class="icon-wrapper">
              <el-icon><Money /></el-icon>
            </div>
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
                <span>预测库存状态分布</span>
              </div>
            </div>
          </template>
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行 -->
    <el-row :gutter="20" class="chart-row">
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
              <div class="stock-info" :class="getStockInfoClass(item)">库存：{{ item.currentStock }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card warning-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" style="color: #E6A23C"><Warning /></el-icon>
                <span>补货预警商品</span>
              </div>
              <el-button type="warning" link @click="goToSuggestion">查看全部 ({{ suggestionSummary.totalCount || 0 }})</el-button>
            </div>
          </template>
          <div class="warning-list">
            <div v-for="item in warningProducts" :key="item.id" class="warning-item" @click="goToSuggestion">
              <div class="warning-left">
                <div class="warning-name">{{ item.productName }}</div>
                <div class="warning-detail">库存 <span class="danger">{{ item.currentStock }}</span> / 安全库存 {{ item.safetyStock }}</div>
              </div>
              <div class="warning-right">
                <div class="suggested-purchase">建议进货 <span class="highlight">+{{ item.suggestedQuantity }}</span></div>
                <el-tag :type="item.lightStatus === 1 ? 'danger' : 'warning'" size="small">{{ item.lightStatus === 1 ? '必须补货' : '顺带补货' }}</el-tag>
              </div>
            </div>
            <div v-if="warningProducts.length === 0" class="no-warning">
              <el-icon><CircleCheck /></el-icon>
              <span>暂无需要补货的商品</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作区 -->
    <el-card shadow="hover" class="action-card">
      <template #header><div class="card-header"><span>快捷操作</span></div></template>
      <div class="action-buttons">
        <div class="action-item" @click="goToPredict">
          <div class="action-icon action-icon-blue"><el-icon><Aim /></el-icon></div>
          <div class="action-text"><div class="action-title">执行预测</div><div class="action-desc">选择商品进行销量预测</div></div>
        </div>
        <div class="action-item" @click="goToSuggestion">
          <div class="action-icon action-icon-orange"><el-icon><ShoppingCart /></el-icon></div>
          <div class="action-text"><div class="action-title">查看进货建议</div><div class="action-desc">智能补货决策支持</div></div>
        </div>
        <div class="action-item" @click="goToAnalysis">
          <div class="action-icon action-icon-green"><el-icon><DataLine /></el-icon></div>
          <div class="action-text"><div class="action-title">预测分析</div><div class="action-desc">查看预测准确率分析</div></div>
        </div>
        <div class="action-item" @click="goToCalendar">
          <div class="action-icon action-icon-purple"><el-icon><Calendar /></el-icon></div>
          <div class="action-text"><div class="action-title">日历因子</div><div class="action-desc">管理节假日和天气数据</div></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { CircleCheck, Goods, TrendCharts, ShoppingCart, Aim, Warning, DataLine, PieChart, Calendar, Money } from '@element-plus/icons-vue'
import { getForecastResults, getForecastSummary, getForecastTrendDaily } from '@/api/forecast'
import { getSuggestionList, getSuggestionSummary } from '@/api/restocking'

const router = useRouter()
const loading = ref(false)
const trendRange = ref('7')

const kpiData = reactive({ totalProducts: 0, totalPredicted: 0, totalPredictedAmount: 0, quantityGrowthRate: null, amountGrowthRate: null })

const trendChartRef = ref(null)
const statusChartRef = ref(null)
let trendChart = null
let statusChart = null

const topProducts = ref([])
const warningProducts = ref([])
const allForecastResults = ref([])

// 进货建议数据
const suggestionSummary = ref({ redCount: 0, yellowCount: 0, totalCount: 0 })
const suggestionList = ref([])

const getRankClass = (index) => index === 0 ? 'rank-1' : index === 1 ? 'rank-2' : index === 2 ? 'rank-3' : ''
const getStockInfoClass = (item) => item.currentStock < 50 ? 'low-stock' : item.currentStock < 100 ? 'medium-stock' : 'enough-stock'
const formatAmount = (amount) => {
  if (amount >= 10000) {
    return (amount / 10000).toFixed(2) + '万'
  }
  return amount.toFixed(2)
}
const formatGrowthRate = (rate) => {
  if (rate === null || rate === undefined) return null
  const num = parseFloat(rate)
  if (num > 0) return { text: `较昨日 +${num}%`, type: 'up' }
  if (num < 0) return { text: `较昨日 ${num}%`, type: 'down' }
  return { text: '较昨日持平', type: 'flat' }
}
const goToPredict = () => router.push('/forecasting/predict')
const goToSuggestion = () => router.push('/restocking/suggestion')
const goToAnalysis = () => router.push('/forecasting/analysis')
const goToCalendar = () => router.push('/sales/calendar')

const loadKpiData = async () => {
  try {
    const res = await getForecastSummary()
    if (res.code === 200 && res.data) {
      const data = res.data || {}
      kpiData.totalProducts = data.totalProducts || 0
      kpiData.totalPredicted = data.totalPredicted || 0
      kpiData.totalPredictedAmount = data.totalPredictedAmount || 0
      kpiData.quantityGrowthRate = data.quantityGrowthRate
      kpiData.amountGrowthRate = data.amountGrowthRate
    }
  } catch (error) { console.error('获取 KPI 数据失败:', error) }
}

const loadForecastResults = async () => {
  try {
    const res = await getForecastResults({ days: 1 })
    // 拦截器已处理，res.code === 200 表示成功，res.data 是后端返回的 data
    if (res.code === 200 && res.data) {
      const results = res.data || []
      allForecastResults.value = results
      results.forEach(item => {
        const dailyAvg = item.historicalDailyAvg || item.predictedQuantity || 0
        const cycle = item.restockCycleDays || 7
        const yellowLine = item.safetyStock + dailyAvg * cycle * 0.3
        if (item.currentStock <= item.safetyStock) {
          item.stockStatus = 'warning';      item.lightStatus = 1  // 红灯：触底
        } else if (item.currentStock <= yellowLine) {
          item.stockStatus = 'needPurchase'; item.lightStatus = 2  // 黄灯：周期70%已消耗
        } else {
          item.stockStatus = 'sufficient';   item.lightStatus = 0  // 绿灯：充足
        }
      })
      topProducts.value = [...results].sort((a, b) => b.predictedQuantity - a.predictedQuantity).slice(0, 10)
    }
  } catch (error) { console.error('获取预测结果失败:', error) }
}

// 加载进货建议数据
const loadSuggestionData = async () => {
  try {
    // 并行获取汇总和列表
    const [summaryRes, listRes] = await Promise.all([
      getSuggestionSummary(),
      getSuggestionList({ status: 0 })
    ])

    if (summaryRes.code === 200 && summaryRes.data) {
      suggestionSummary.value = summaryRes.data
    }

    if (listRes.code === 200 && listRes.data) {
      suggestionList.value = listRes.data
      // 取前5个作为预警商品
      warningProducts.value = listRes.data.slice(0, 5).map(item => ({
        ...item,
        suggestedPurchase: item.suggestedQuantity
      }))
    }
  } catch (error) { console.error('获取进货建议失败:', error) }
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  try {
    const days = parseInt(trendRange.value)
    const endDate = new Date()
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - days + 1)

    const res = await getForecastTrendDaily({
      startDate: formatDate(startDate),
      endDate: formatDate(endDate)
    })

    const trendData = res.data || []
    const dates = trendData.map(item => item.date.substring(5))
    // 销售额（保留 2 位小数，便于图表展示）
    const predicted = trendData.map(item => Number(Number(item.predictedAmount || 0).toFixed(2)))
    const actual = trendData.map(item => Number(Number(item.actualAmount || 0).toFixed(2)))

    // Y 轴刻度格式化：≥10000 → X.X万，否则原值
    const formatYAxis = (val) => {
      if (val >= 10000) return (val / 10000).toFixed(1) + '万'
      return val
    }
    // tooltip 金额格式化
    const formatMoney = (val) => {
      const n = Number(val) || 0
      if (n >= 10000) return '¥' + (n / 10000).toFixed(2) + '万'
      return '¥' + n.toFixed(2)
    }

    const option = {
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: '#e4e7ed',
        textStyle: { color: '#303133' },
        formatter: (params) => {
          if (!params || params.length === 0) return ''
          let html = `<div style="font-weight:600;margin-bottom:4px;">${params[0].axisValue}</div>`
          params.forEach(p => {
            html += `<div style="display:flex;align-items:center;margin:2px 0;">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color};margin-right:8px;"></span>
              <span style="flex:1;">${p.seriesName}</span>
              <span style="font-weight:600;margin-left:16px;">${formatMoney(p.value)}</span>
            </div>`
          })
          return html
        }
      },
      legend: { data: ['实际销售额', '预测销售额'], top: 10, textStyle: { color: '#606266' } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: 60, containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dates, axisLine: { lineStyle: { color: '#e4e7ed' } }, axisLabel: { color: '#909399' } },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisLabel: { color: '#909399', formatter: formatYAxis },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
      },
      series: [
        { name: '实际销售额', type: 'line', smooth: true, symbol: 'circle', symbolSize: 8, data: actual, lineStyle: { color: '#409EFF', width: 3 }, itemStyle: { color: '#409EFF' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.05)' }]) } },
        { name: '预测销售额', type: 'line', smooth: true, symbol: 'diamond', symbolSize: 8, data: predicted, lineStyle: { color: '#67C23A', width: 3, type: 'dashed' }, itemStyle: { color: '#67C23A' } }
      ]
    }
    trendChart.setOption(option)
  } catch (error) {
    console.error('加载趋势数据失败:', error)
    ElMessage.warning('暂无实际销售数据，图表显示预测数据')
  }
}

const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const initStatusChart = () => {
  if (!statusChartRef.value) return
  if (!statusChart) statusChart = echarts.init(statusChartRef.value)

  // 从实时预测结果统计三灯数量，与销量预测页保持一致
  const redCount    = allForecastResults.value.filter(i => i.stockStatus === 'warning').length
  const yellowCount = allForecastResults.value.filter(i => i.stockStatus === 'needPurchase').length
  const greenCount  = allForecastResults.value.filter(i => i.stockStatus === 'sufficient').length

  const option = {
    tooltip: { 
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: { 
      orient: 'vertical', 
      right: 15, 
      top: 15, 
      textStyle: { color: '#606266' } 
    },
    series: [{
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['35%', '50%'],
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
          show: false
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      labelLine: { show: false },
      data: [
        { value: greenCount, name: '库存充足', itemStyle: { color: '#67C23A' } },
        { value: yellowCount, name: '库存紧张', itemStyle: { color: '#E6A23C' } },
        { value: redCount, name: '库存告急', itemStyle: { color: '#F56C6C' } }
      ]
    }],
    // 添加图形中心文字
    graphic: {
      type: 'group',
      left: '35%',
      top: '45%',
      children: [
        {
          type: 'text',
          style: {
            text: '商品总数',
            textAlign: 'center',
            textLineHeight: 20,
            fill: '#909399',
            font: '13px sans-serif'
          },
          top: -10
        },
        {
          type: 'text',
          style: {
            text: (redCount + yellowCount + greenCount).toString(),
            textAlign: 'center',
            textLineHeight: 36,
            fill: '#1e293b',
            font: 'bold 28px sans-serif',
            textStroke: '#fff',
            textStrokeWidth: 3
          },
          top: 5
        }
      ]
    }
  }
  statusChart.setOption(option)
}

const initCharts = async () => {
  await nextTick()
  await initTrendChart()
  initStatusChart()
}
const handleResize = () => { trendChart?.resize(); statusChart?.resize() }
watch(trendRange, () => { initTrendChart() })

const loadAllData = async () => {
  loading.value = true
  try {
    await Promise.all([loadKpiData(), loadForecastResults(), loadSuggestionData()])
    initCharts()
  } catch (error) { console.error('加载数据失败:', error); ElMessage.error('加载数据失败，请稍后重试') }
  finally { loading.value = false }
}

onMounted(() => { loadAllData(); window.addEventListener('resize', handleResize) })
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); trendChart?.dispose(); statusChart?.dispose() })
</script>

<style scoped lang="scss">
.forecast-dashboard { padding: 24px; background-color: #f8fafc; min-height: calc(100vh - 100px); }
.header-section { margin-bottom: 24px; display: flex; justify-content: space-between; align-items: flex-start; .title { font-size: 24px; font-weight: 800; color: #1e293b; margin: 0; } .subtitle { font-size: 14px; color: #64748b; margin: 8px 0 0; } .model-info { display: flex; gap: 10px; .model-tag, .status-tag { display: flex; align-items: center; gap: 4px; padding: 8px 12px; font-weight: 500; } } }
.kpi-section { margin-bottom: 24px; .kpi-card { border-radius: 16px; border: none; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04); transition: all 0.3s ease; overflow: hidden; &.clickable { cursor: pointer; &:hover { transform: translateY(-4px); box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08); } } :deep(.el-card__body) { padding: 20px; } .kpi-card-content { display: flex; justify-content: space-between; align-items: center; .kpi-text { .kpi-label { font-size: 14px; color: #64748b; margin-bottom: 8px; } .kpi-value { font-size: 32px; font-weight: 800; color: #1e293b; margin-bottom: 8px; &.warning { color: #E6A23C; } } .kpi-trend { font-size: 12px; color: #909399; display: flex; align-items: center; gap: 4px; &.up { color: #67C23A; } &.down { color: #F56C6C; } &.flat { color: #909399; } } .kpi-trend-placeholder { height: 20px; } } .icon-wrapper { width: 64px; height: 64px; border-radius: 16px; display: flex; justify-content: center; align-items: center; font-size: 28px; } } &.kpi-blue { background: linear-gradient(135deg, #EBF5FF 0%, #DBEAFE 100%); .icon-wrapper { background: rgba(59, 130, 246, 0.15); color: #3B82F6; } } &.kpi-green { background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%); .icon-wrapper { background: rgba(16, 185, 129, 0.15); color: #10B981; } } &.kpi-orange { background: linear-gradient(135deg, #FFF7ED 0%, #FFEDD5 100%); .icon-wrapper { background: rgba(245, 158, 11, 0.15); color: #F59E0B; } } &.kpi-purple { background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%); .icon-wrapper { background: rgba(139, 92, 246, 0.15); color: #8B5CF6; } } } }
.chart-row { margin-bottom: 20px; }
.chart-card { border-radius: 16px; border: none; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04); :deep(.el-card__header) { border-bottom: 1px solid #f1f5f9; padding: 16px 20px; } .card-header { display: flex; justify-content: space-between; align-items: center; .header-left { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 700; color: #334155; .header-icon { font-size: 20px; color: #409EFF; } } } .chart-box { height: 300px; width: 100%; } }
.action-card { border-radius: 16px; border: none; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04); :deep(.el-card__header) { border-bottom: 1px solid #f1f5f9; padding: 16px 20px; font-size: 16px; font-weight: 700; color: #334155; } .action-buttons { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; padding: 10px 0; } .action-item { display: flex; align-items: center; gap: 16px; padding: 16px 20px; background: #f8fafc; border-radius: 12px; cursor: pointer; transition: all 0.3s ease; &:hover { background: #f1f5f9; transform: translateY(-2px); } .action-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-size: 24px; &.action-icon-blue { background: rgba(59, 130, 246, 0.1); color: #3B82F6; } &.action-icon-orange { background: rgba(245, 158, 11, 0.1); color: #F59E0B; } &.action-icon-green { background: rgba(16, 185, 129, 0.1); color: #10B981; } &.action-icon-purple { background: rgba(139, 92, 246, 0.1); color: #8B5CF6; } } .action-text { .action-title { font-size: 15px; font-weight: 600; color: #1e293b; margin-bottom: 4px; } .action-desc { font-size: 12px; color: #64748b; } } } }
.top-product-list { max-height: 340px; overflow-y: auto; .top-product-item { display: flex; align-items: center; padding: 12px 16px; border-radius: 10px; transition: all 0.2s ease; margin-bottom: 8px; background: #f8fafc; &:hover { background: #f1f5f9; transform: translateX(4px); } .rank { width: 28px; height: 28px; border-radius: 8px; display: flex; justify-content: center; align-items: center; font-size: 14px; font-weight: 700; background: #e2e8f0; color: #64748b; margin-right: 12px; flex-shrink: 0; &.rank-1 { background: linear-gradient(135deg, #F56C6C, #E6A23C); color: #fff; } &.rank-2 { background: linear-gradient(135deg, #909399, #C0C4CC); color: #fff; } &.rank-3 { background: linear-gradient(135deg, #E6A23C, #F5D442); color: #fff; } } .product-info { flex: 1; min-width: 0; .product-name { font-size: 14px; font-weight: 600; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; } .product-category { font-size: 12px; color: #94a3b8; margin-top: 2px; } } .predict-value { text-align: right; margin-right: 16px; .value { font-size: 18px; font-weight: 700; color: #F56C6C; } .unit { font-size: 12px; color: #94a3b8; margin-left: 2px; } } .stock-info { font-size: 12px; padding: 4px 8px; border-radius: 6px; font-weight: 500; &.low-stock { background: #FEF0F0; color: #F56C6C; } &.medium-stock { background: #FDF6EC; color: #E6A23C; } &.enough-stock { background: #F0F9EB; color: #67C23A; } } } }
.warning-card { .warning-list { max-height: 340px; overflow-y: auto; .warning-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-radius: 10px; background: #FFFBEB; border-left: 4px solid #E6A23C; margin-bottom: 10px; cursor: pointer; transition: all 0.2s ease; &:hover { transform: translateX(4px); box-shadow: 0 4px 12px rgba(230, 162, 60, 0.15); } .warning-left { flex: 1; .warning-name { font-size: 14px; font-weight: 600; color: #1e293b; margin-bottom: 4px; } .warning-detail { font-size: 12px; color: #64748b; .danger { color: #F56C6C; font-weight: 600; } } } .warning-right { text-align: right; .suggested-purchase { font-size: 12px; color: #64748b; margin-bottom: 6px; .highlight { font-size: 16px; font-weight: 700; color: #F56C6C; } } } } .no-warning { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; color: #67C23A; .el-icon { font-size: 48px; margin-bottom: 12px; } span { font-size: 14px; color: #64748b; } } } }
</style>
