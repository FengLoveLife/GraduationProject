<template>
  <div class="dashboard">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-left">
        <h1 class="welcome-title">
          <span class="greeting">{{ greeting }}</span>
          <span class="name">，{{ userName }}</span>
        </h1>
        <p class="welcome-subtitle">{{ todayStr }} · {{ weekDay }}</p>
      </div>
    </div>

    <!-- 维度一：核心 KPI 数据卡片 -->
    <div class="kpi-section">
      <div class="kpi-card kpi-blue">
        <div class="kpi-icon">
          <el-icon><Money /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">
            <span class="currency">¥</span>
            <span class="number">{{ formatNumber(kpiData.todaySales) }}</span>
          </div>
          <div class="kpi-label">今日营业额</div>
          <div class="kpi-trend" :class="trendClass(kpiData.salesGrowth)">
            <el-icon v-if="kpiData.salesGrowth > 0"><Top /></el-icon>
            <el-icon v-else-if="kpiData.salesGrowth < 0"><Bottom /></el-icon>
            较昨日 {{ trendText(kpiData.salesGrowth) }}
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-green">
        <div class="kpi-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">
            <span class="number">{{ kpiData.todayOrders }}</span>
            <span class="unit">单</span>
          </div>
          <div class="kpi-label">今日订单数</div>
          <div class="kpi-trend" :class="trendClass(kpiData.orderGrowth)">
            <el-icon v-if="kpiData.orderGrowth > 0"><Top /></el-icon>
            <el-icon v-else-if="kpiData.orderGrowth < 0"><Bottom /></el-icon>
            较昨日 {{ trendText(kpiData.orderGrowth) }}
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-orange clickable" @click="goTo('/inventory/dashboard')">
        <div class="kpi-icon warning">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value warning">
            <span class="number">{{ kpiData.stockWarning }}</span>
            <span class="unit">种</span>
          </div>
          <div class="kpi-label">库存告急预警</div>
          <div class="kpi-trend">
            <el-icon><Bell /></el-icon>
            点击查看详情
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-purple clickable" @click="goTo('/restocking/records')">
        <div class="kpi-icon">
          <el-icon><ShoppingCart /></el-icon>
        </div>
        <div class="kpi-content">
          <div class="kpi-value">
            <span class="number">{{ kpiData.pendingPurchase }}</span>
            <span class="unit">笔</span>
          </div>
          <div class="kpi-label">待处理进货单</div>
          <div class="kpi-trend">
            <el-icon><Clock /></el-icon>
            待确认/已下单
          </div>
        </div>
      </div>
    </div>

    <!-- 维度二：可视化图表区 -->
    <div class="chart-section">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-card class="chart-card main-chart" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <el-icon class="header-icon"><TrendCharts /></el-icon>
                  <span>近7天销售额 vs AI预测趋势</span>
                </div>
              </div>
            </template>
            <div ref="salesChartRef" class="chart-box"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <el-icon class="header-icon" style="color: #E6A23C"><PieChart /></el-icon>
                  <span>{{ categoryPieLabel }}品类销量占比</span>
                </div>
              </div>
            </template>
            <div ref="categoryChartRef" class="chart-box"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 维度三：快捷操作台 -->
    <div class="action-section">
      <el-card class="quick-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon class="header-icon" style="color: #409EFF"><Operation /></el-icon>
              <span>快捷操作台</span>
            </div>
          </div>
        </template>
        <div class="quick-grid">
          <div class="quick-item" @click="goTo('/sales/import')">
            <div class="quick-icon quick-blue">
              <el-icon><Upload /></el-icon>
            </div>
            <div class="quick-text">
              <div class="quick-title">导入日结单</div>
              <div class="quick-desc">上传销售Excel</div>
            </div>
          </div>
          <div class="quick-item" @click="goTo('/forecasting/predict')">
            <div class="quick-icon quick-green">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="quick-text">
              <div class="quick-title">运行AI预测</div>
              <div class="quick-desc">智能销量预测</div>
            </div>
          </div>
          <div class="quick-item" @click="goTo('/restocking/suggestion')">
            <div class="quick-icon quick-orange">
              <el-icon><ShoppingCart /></el-icon>
            </div>
            <div class="quick-text">
              <div class="quick-title">审核进货单</div>
              <div class="quick-desc">AI进货建议</div>
            </div>
          </div>
          <div class="quick-item" @click="goTo('/inventory/dashboard')">
            <div class="quick-icon quick-purple">
              <el-icon><Box /></el-icon>
            </div>
            <div class="quick-text">
              <div class="quick-title">盘点库存</div>
              <div class="quick-desc">实时库存监控</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Money, Document, Warning, ShoppingCart, Top, Bottom, Bell, Clock,
  TrendCharts, PieChart, Cpu, Operation, Upload, Box
} from '@element-plus/icons-vue'
import { getDashboardData } from '@/api/dashboard'
import { getProfile } from '@/api/user'

const router = useRouter()

// 用户信息
const userName = ref('')

// 加载状态
const loading = ref(false)

// 时间相关
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayStr = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})

const weekDay = computed(() => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[new Date().getDay()]
})

// KPI 数据（从后端获取）
const kpiData = reactive({
  todaySales: 0,
  salesGrowth: 0,
  todayOrders: 0,
  orderGrowth: 0,
  stockWarning: 0,
  pendingPurchase: 0
})

// 图表数据（从后端获取）
const salesTrendData = ref([])
const categoryPieData = ref([])
const categoryPieLabel = ref('今日')

// 图表引用
const salesChartRef = ref(null)
const categoryChartRef = ref(null)
let salesChart = null
let categoryChart = null

// 格式化数字
const formatNumber = (num) => {
  return num.toLocaleString('zh-CN')
}

// 趋势文字：正数显示 +X%，负数显示 -X%，零显示 0%
const trendText = (val) => {
  if (val > 0) return `+${val}%`
  if (val < 0) return `${val}%`
  return '0%'
}

// 趋势样式：上涨=红，下跌=绿，持平=灰
const trendClass = (val) => {
  if (val > 0) return 'up'
  if (val < 0) return 'down'
  return 'flat'
}

// 跳转
const goTo = (path) => {
  router.push(path)
}

// 获取后端数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDashboardData()
    if (res.code === 200 && res.data) {
      // 更新 KPI 数据
      kpiData.todaySales = res.data.todaySales || 0
      kpiData.salesGrowth = res.data.salesGrowth || 0
      kpiData.todayOrders = res.data.todayOrders || 0
      kpiData.orderGrowth = res.data.orderGrowth || 0
      kpiData.stockWarning = res.data.stockWarning || 0
      kpiData.pendingPurchase = res.data.pendingPurchase || 0

      // 更新图表数据
      salesTrendData.value = res.data.salesTrend || []
      categoryPieData.value = res.data.categoryPie || []
      // 读取数据来源标签（后端将 label 放在第一条记录）
      if (categoryPieData.value.length > 0 && categoryPieData.value[0].label) {
        categoryPieLabel.value = categoryPieData.value[0].label
      }

      // 初始化图表
      nextTick(() => {
        initSalesChart()
        initCategoryChart()
      })
    }
  } catch (error) {
    console.error('获取首页数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 初始化销售趋势图
const initSalesChart = () => {
  if (!salesChartRef.value) return
  if (!salesChart) salesChart = echarts.init(salesChartRef.value)

  const trendData = salesTrendData.value
  if (trendData.length === 0) return

  const dates = trendData.map(d => d.date)

  // 历史实际销售额（仅历史天有值）
  const actualData = trendData.map(d => d.isPrediction ? null : d.actualAmount)

  // 预测线：历史天用 predictedAmount，未来天用 predictedAmount，全程连贯
  const predictedData = trendData.map(d => {
    if (d.predictedAmount !== null && d.predictedAmount !== undefined) return d.predictedAmount
    return null
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' },
      formatter: function(params) {
        const date = params[0].axisValue
        const idx = params[0].dataIndex
        const actual = actualData[idx]
        const predicted = predictedData[idx]
        const isFuture = trendData[idx] && trendData[idx].isPrediction

        let html = `<div style="font-weight:600;margin-bottom:6px">${date}</div>`
        if (!isFuture && actual !== null && actual !== undefined) {
          html += `<div style="font-size:13px;color:#3B82F6;font-weight:700">实际 ¥${Number(actual).toLocaleString()}</div>`
        }
        if (predicted !== null && predicted !== undefined) {
          const label = isFuture ? 'AI预测' : 'AI预测'
          html += `<div style="font-size:13px;color:#67C23A;font-weight:700">${label} ¥${Number(predicted).toLocaleString()}</div>`
        }
        if (!isFuture && actual !== null && predicted !== null && actual !== undefined && predicted !== undefined) {
          const diff = ((Number(actual) - Number(predicted)) / Number(predicted) * 100).toFixed(1)
          const color = Math.abs(diff) <= 10 ? '#10B981' : '#F59E0B'
          html += `<div style="font-size:12px;color:${color};margin-top:3px">偏差 ${diff > 0 ? '+' : ''}${diff}%</div>`
        }
        return html
      }
    },
    legend: {
      data: ['历史销售额', 'AI预测值'],
      top: 10,
      textStyle: { color: '#606266' }
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', top: 60,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: {
        color: '#909399',
        formatter: v => v >= 10000 ? (v/10000) + 'w' : v
      },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [
      {
        name: '历史销售额',
        type: 'line',
        data: actualData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#3B82F6', width: 3 },
        itemStyle: { color: '#3B82F6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.35)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ])
        }
      },
      {
        name: 'AI预测值',
        type: 'line',
        data: predictedData,
        smooth: true,
        symbol: 'diamond',
        symbolSize: 10,
        lineStyle: { color: '#67C23A', width: 3, type: 'dashed' },
        itemStyle: { color: '#67C23A' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.2)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.02)' }
          ])
        }
      }
    ]
  }
  salesChart.setOption(option)
}

// 初始化品类饼图
const initCategoryChart = () => {
  if (!categoryChartRef.value) return
  if (!categoryChart) categoryChart = echarts.init(categoryChartRef.value)

  const pieData = categoryPieData.value
  if (pieData.length === 0) return

  // 配色方案
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#8B5CF6']

  const option = {
    tooltip: { show: false },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: '#606266', fontSize: 12 }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      labelLine: { show: false },
      emphasis: {
        label: {
          show: true,
          formatter: '{b}\n{c}件 | {d}%',
          fontSize: 14,
          fontWeight: 'bold',
          color: '#1e293b',
          lineHeight: 22
        },
        scaleSize: 6
      },
      data: pieData.map((item, index) => ({
        value: item.value,
        name: item.name,
        itemStyle: { color: colors[index % colors.length] }
      }))
    }]
  }
  categoryChart.setOption(option)
}

// 窗口resize
const handleResize = () => {
  salesChart?.resize()
  categoryChart?.resize()
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await getProfile()
    if (res.code === 200 && res.data) {
      userName.value = res.data.realName || res.data.username || '店长'
    }
  } catch (error) {
    userName.value = '店长'
  }
}

onMounted(() => {
  fetchUserInfo()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  salesChart?.dispose()
  categoryChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 24px;
  background: linear-gradient(180deg, #f0f5ff 0%, #f8fafc 100%);
  min-height: calc(100vh - 100px);
}

// 欢迎区域
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 28px;
  padding: 0 4px;

  .welcome-title {
    font-size: 28px;
    font-weight: 800;
    color: #1e293b;
    margin: 0 0 8px 0;

    .greeting {
      background: linear-gradient(135deg, #3B82F6, #8B5CF6);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }

  .welcome-subtitle {
    font-size: 14px;
    color: #64748b;
    margin: 0;
  }

}

// KPI 卡片
.kpi-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.kpi-card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.02);

  &.clickable {
    cursor: pointer;
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
    }
  }

  .kpi-icon {
    width: 56px;
    height: 56px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    flex-shrink: 0;

    &.warning {
      animation: shake 0.5s ease-in-out infinite;
    }
  }

  .kpi-content {
    flex: 1;
    min-width: 0;

    .kpi-value {
      display: flex;
      align-items: baseline;
      gap: 2px;
      margin-bottom: 6px;

      .currency {
        font-size: 18px;
        font-weight: 700;
        color: #1e293b;
      }

      .number {
        font-size: 32px;
        font-weight: 800;
        color: #1e293b;
        line-height: 1;
      }

      .unit {
        font-size: 14px;
        font-weight: 500;
        color: #64748b;
        margin-left: 4px;
      }

      &.warning .number {
        color: #F59E0B;
      }
    }

    .kpi-label {
      font-size: 14px;
      color: #64748b;
      margin-bottom: 8px;
    }

    .kpi-trend {
      font-size: 12px;
      color: #909399;
      display: flex;
      align-items: center;
      gap: 4px;

      &.up {
        color: #F56C6C;
      }

      &.down {
        color: #10B981;
      }

      &.flat {
        color: #909399;
      }
    }
  }

  &.kpi-blue {
    .kpi-icon {
      background: linear-gradient(135deg, #EBF5FF, #DBEAFE);
      color: #3B82F6;
    }
  }

  &.kpi-green {
    .kpi-icon {
      background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
      color: #10B981;
    }
  }

  &.kpi-orange {
    .kpi-icon {
      background: linear-gradient(135deg, #FFF7ED, #FFEDD5);
      color: #F59E0B;
    }
  }

  &.kpi-purple {
    .kpi-icon {
      background: linear-gradient(135deg, #F5F3FF, #EDE9FE);
      color: #8B5CF6;
    }
  }
}

@keyframes shake {
  0%, 100% { transform: rotate(0); }
  25% { transform: rotate(-5deg); }
  75% { transform: rotate(5deg); }
}

// 图表区域
.chart-section {
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 20px;
  border: none;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);

  :deep(.el-card__header) {
    border-bottom: 1px solid #f1f5f9;
    padding: 18px 24px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 16px;
      font-weight: 700;
      color: #334155;

      .header-icon {
        font-size: 20px;
        color: #409EFF;
      }
    }
  }

  .chart-box {
    height: 320px;
    width: 100%;
  }

  &.main-chart {
    .chart-box {
      height: 320px;
    }
  }
}

// 智能行动区域
.action-section {
  .el-card {
    border-radius: 20px;
    border: none;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  }

  :deep(.el-card__header) {
    border-bottom: 1px solid #f1f5f9;
    padding: 18px 24px;
  }
}

// 快捷操作
.quick-card {
  .card-header {
    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 16px;
      font-weight: 700;
      color: #334155;

      .header-icon {
        font-size: 20px;
      }
    }
  }

  .quick-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }

  .quick-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px;
    background: #f8fafc;
    border-radius: 14px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: #f1f5f9;
      transform: translateY(-2px);

      .quick-icon {
        transform: scale(1.1);
      }
    }

    .quick-icon {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      transition: transform 0.3s ease;

      &.quick-blue {
        background: linear-gradient(135deg, #EBF5FF, #DBEAFE);
        color: #3B82F6;
      }

      &.quick-green {
        background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
        color: #10B981;
      }

      &.quick-orange {
        background: linear-gradient(135deg, #FFF7ED, #FFEDD5);
        color: #F59E0B;
      }

      &.quick-purple {
        background: linear-gradient(135deg, #F5F3FF, #EDE9FE);
        color: #8B5CF6;
      }
    }

    .quick-text {
      .quick-title {
        font-size: 15px;
        font-weight: 600;
        color: #1e293b;
        margin-bottom: 4px;
      }

      .quick-desc {
        font-size: 12px;
        color: #64748b;
      }
    }
  }
}
</style>