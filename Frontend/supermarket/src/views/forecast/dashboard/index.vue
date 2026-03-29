<template>
  <div class="forecast-dashboard" v-loading="loading">
    <!-- 头部标题 -->
    <div class="header-section">
      <div class="header-left">
        <h2 class="title">智能销量预测中心</h2>
        <p class="subtitle">基于 LightGBM 机器学习模型，精准预测商品销量趋势</p>
      </div>
      <div class="header-right">
        <div class="model-info">
          <el-tag type="success" effect="plain" class="model-tag">
            <el-icon><Cpu /></el-icon>
            <span>LightGBM 模型</span>
          </el-tag>
          <el-tag :type="modelStatus === 'running' ? 'success' : 'danger'" effect="light" class="status-tag">
            <el-icon><component :is="modelStatus === 'running' ? 'CircleCheck' : 'CircleClose'" /></el-icon>
            <span>{{ modelStatus === 'running' ? '模型运行中' : '演示模式' }}</span>
          </el-tag>
        </div>
      </div>
    </div>

    <!-- KPI 卡片区 -->
    <el-row :gutter="20" class="kpi-section">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-blue">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">今日预测商品</div>
              <div class="kpi-value">{{ kpiData.totalProducts }}</div>
              <div class="kpi-trend up">
                <el-icon><Top /></el-icon>
                较昨日 +{{ kpiData.productGrowth }}
              </div>
            </div>
            <div class="icon-wrapper">
              <el-icon><Goods /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-green">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">预测总销量</div>
              <div class="kpi-value">{{ kpiData.totalPredicted }}</div>
              <div class="kpi-trend up">
                <el-icon><Top /></el-icon>
                预计增长 {{ kpiData.growthRate }}%
              </div>
            </div>
            <div class="icon-wrapper">
              <el-icon><TrendCharts /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-orange clickable" @click="goToSuggestion">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">需补货商品</div>
              <div class="kpi-value warning">{{ kpiData.needRestock }}</div>
              <div class="kpi-trend">
                <el-icon><Warning /></el-icon>
                点击查看详情
              </div>
            </div>
            <div class="icon-wrapper">
              <el-icon><ShoppingCart /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-purple">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">预测准确率</div>
              <div class="kpi-value">{{ kpiData.accuracy }}%</div>
              <div class="kpi-trend up">
                <el-icon><CircleCheck /></el-icon>
                MAPE: {{ kpiData.mape }}%
              </div>
            </div>
            <div class="icon-wrapper">
              <el-icon><Aim /></el-icon>
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
                <span>预测销量趋势 vs 实际销量</span>
              </div>
              <div class="header-right">
                <el-radio-group v-model="trendRange" size="small">
                  <el-radio-button label="7">近7天</el-radio-button>
                  <el-radio-button label="14">近14天</el-radio-button>
                  <el-radio-button label="30">近30天</el-radio-button>
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

    <!-- 第二行图表 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" style="color: #409EFF"><Histogram /></el-icon>
                <span>分类预测销量排行</span>
              </div>
            </div>
          </template>
          <div ref="categoryChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" style="color: #67C23A"><Calendar /></el-icon>
                <span>预测影响因素权重</span>
              </div>
            </div>
          </template>
          <div ref="factorChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作区 -->
    <el-card shadow="hover" class="action-card">
      <template #header>
        <div class="card-header">
          <span>快捷操作</span>
        </div>
      </template>
      <div class="action-buttons">
        <div class="action-item" @click="goToPredict">
          <div class="action-icon action-icon-blue">
            <el-icon><Aim /></el-icon>
          </div>
          <div class="action-text">
            <div class="action-title">执行预测</div>
            <div class="action-desc">选择商品进行销量预测</div>
          </div>
        </div>
        <div class="action-item" @click="goToSuggestion">
          <div class="action-icon action-icon-orange">
            <el-icon><ShoppingCart /></el-icon>
          </div>
          <div class="action-text">
            <div class="action-title">查看进货建议</div>
            <div class="action-desc">{{ kpiData.needRestock }} 种商品需要补货</div>
          </div>
        </div>
        <div class="action-item" @click="goToAnalysis">
          <div class="action-icon action-icon-green">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="action-text">
            <div class="action-title">预测分析</div>
            <div class="action-desc">查看预测准确率分析</div>
          </div>
        </div>
        <div class="action-item" @click="goToCalendar">
          <div class="action-icon action-icon-purple">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="action-text">
            <div class="action-title">日历因子</div>
            <div class="action-desc">管理节假日和天气数据</div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Cpu, CircleCheck, CircleClose, Goods, TrendCharts, ShoppingCart, Aim,
  Top, Warning, DataLine, PieChart, Calendar, Histogram
} from '@element-plus/icons-vue'

const router = useRouter()

// 加载状态
const loading = ref(false)
const modelStatus = ref('demo') // 'running' | 'demo'
const trendRange = ref('7')

// KPI 数据
const kpiData = reactive({
  totalProducts: 186,
  productGrowth: 12,
  totalPredicted: 2847,
  growthRate: 8.5,
  needRestock: 23,
  accuracy: 87.6,
  mape: 12.4
})

// 图表引用
const trendChartRef = ref(null)
const statusChartRef = ref(null)
const categoryChartRef = ref(null)
const factorChartRef = ref(null)

let trendChart = null
let statusChart = null
let categoryChart = null
let factorChart = null

// 跳转方法
const goToPredict = () => router.push('/forecasting/predict')
const goToSuggestion = () => router.push('/restocking/suggestion')
const goToAnalysis = () => router.push('/forecasting/analysis')
const goToCalendar = () => router.push('/sales/calendar')

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  const days = trendRange.value === '7' ? 7 : trendRange.value === '14' ? 14 : 30

  // 生成模拟日期
  const dates = []
  const today = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }

  // 模拟数据
  const actual = [245, 268, 289, 302, 278, 256, 287].slice(0, days)
  const predicted = [252, 261, 295, 298, 285, 262, 280].slice(0, days)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' }
    },
    legend: {
      data: ['实际销量', '预测销量'],
      top: 10,
      textStyle: { color: '#606266' }
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', top: 60,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399' }
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
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: actual,
        lineStyle: { color: '#409EFF', width: 3 },
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.05)' }
          ])
        }
      },
      {
        name: '预测销量',
        type: 'line',
        smooth: true,
        symbol: 'diamond',
        symbolSize: 8,
        data: predicted,
        lineStyle: { color: '#67C23A', width: 3, type: 'dashed' },
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
  trendChart.setOption(option)
}

// 初始化状态分布图
const initStatusChart = () => {
  if (!statusChartRef.value) return
  if (!statusChart) statusChart = echarts.init(statusChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: '#606266' }
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        },
        data: [
          { value: 142, name: '库存充足', itemStyle: { color: '#67C23A' } },
          { value: 21, name: '库存紧张', itemStyle: { color: '#E6A23C' } },
          { value: 23, name: '需要补货', itemStyle: { color: '#F56C6C' } }
        ]
      }
    ]
  }
  statusChart.setOption(option)
}

// 初始化分类排行图
const initCategoryChart = () => {
  if (!categoryChartRef.value) return
  if (!categoryChart) categoryChart = echarts.init(categoryChartRef.value)

  const data = [
    { name: '饮料', value: 523 },
    { name: '休闲零食', value: 412 },
    { name: '方便食品', value: 387 },
    { name: '乳制品', value: 356 },
    { name: '日用清洁', value: 298 },
    { name: '糖果巧克力', value: 245 }
  ].reverse()

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', top: 10,
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: data.map(d => d.name),
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#606266' }
    },
    series: [
      {
        type: 'bar',
        data: data.map(d => d.value),
        barWidth: 20,
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#79bbff' }
          ])
        }
      }
    ]
  }
  categoryChart.setOption(option)
}

// 初始化影响因素图
const initFactorChart = () => {
  if (!factorChartRef.value) return
  if (!factorChart) factorChart = echarts.init(factorChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', top: 30,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['历史销量', '星期', '节假日', '天气', '月份', '促销'],
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#606266', rotate: 0 }
    },
    yAxis: {
      type: 'value',
      name: '权重',
      axisLine: { show: false },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [
      {
        type: 'bar',
        data: [85, 72, 68, 45, 38, 62],
        barWidth: 30,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: function(params) {
            const colors = ['#67C23A', '#409EFF', '#E6A23C', '#909399', '#F56C6C', '#9B59B6']
            return colors[params.dataIndex]
          }
        },
        label: {
          show: true,
          position: 'top',
          color: '#606266'
        }
      }
    ]
  }
  factorChart.setOption(option)
}

// 初始化所有图表
const initCharts = () => {
  nextTick(() => {
    initTrendChart()
    initStatusChart()
    initCategoryChart()
    initFactorChart()
  })
}

// 窗口大小变化处理
const handleResize = () => {
  trendChart?.resize()
  statusChart?.resize()
  categoryChart?.resize()
  factorChart?.resize()
}

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    initCharts()
  }, 500)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  statusChart?.dispose()
  categoryChart?.dispose()
  factorChart?.dispose()
})
</script>

<style scoped lang="scss">
.forecast-dashboard {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 100px);
}

.header-section {
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

  .model-info {
    display: flex;
    gap: 10px;

    .model-tag, .status-tag {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 8px 12px;
      font-weight: 500;
    }
  }
}

.kpi-section {
  margin-bottom: 24px;

  .kpi-card {
    border-radius: 16px;
    border: none;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
    transition: all 0.3s ease;
    overflow: hidden;

    &.clickable {
      cursor: pointer;
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
      }
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .kpi-card-content {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .kpi-text {
        .kpi-label {
          font-size: 14px;
          color: #64748b;
          margin-bottom: 8px;
        }
        .kpi-value {
          font-size: 32px;
          font-weight: 800;
          color: #1e293b;
          margin-bottom: 8px;
          &.warning { color: #E6A23C; }
        }
        .kpi-trend {
          font-size: 12px;
          color: #909399;
          display: flex;
          align-items: center;
          gap: 4px;
          &.up { color: #67C23A; }
          &.down { color: #F56C6C; }
        }
      }

      .icon-wrapper {
        width: 64px;
        height: 64px;
        border-radius: 16px;
        display: flex;
        justify-content: center;
        align-items: center;
        font-size: 28px;
      }
    }

    &.kpi-blue {
      background: linear-gradient(135deg, #EBF5FF 0%, #DBEAFE 100%);
      .icon-wrapper { background: rgba(59, 130, 246, 0.15); color: #3B82F6; }
    }
    &.kpi-green {
      background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%);
      .icon-wrapper { background: rgba(16, 185, 129, 0.15); color: #10B981; }
    }
    &.kpi-orange {
      background: linear-gradient(135deg, #FFF7ED 0%, #FFEDD5 100%);
      .icon-wrapper { background: rgba(245, 158, 11, 0.15); color: #F59E0B; }
    }
    &.kpi-purple {
      background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
      .icon-wrapper { background: rgba(139, 92, 246, 0.15); color: #8B5CF6; }
    }
  }
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
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
      gap: 8px;
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
    height: 300px;
    width: 100%;
  }
}

.action-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);

  :deep(.el-card__header) {
    border-bottom: 1px solid #f1f5f9;
    padding: 16px 20px;
    font-size: 16px;
    font-weight: 700;
    color: #334155;
  }

  .action-buttons {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    padding: 10px 0;
  }

  .action-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px 20px;
    background: #f8fafc;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: #f1f5f9;
      transform: translateY(-2px);
    }

    .action-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      justify-content: center;
      align-items: center;
      font-size: 24px;

      &.action-icon-blue { background: rgba(59, 130, 246, 0.1); color: #3B82F6; }
      &.action-icon-orange { background: rgba(245, 158, 11, 0.1); color: #F59E0B; }
      &.action-icon-green { background: rgba(16, 185, 129, 0.1); color: #10B981; }
      &.action-icon-purple { background: rgba(139, 92, 246, 0.1); color: #8B5CF6; }
    }

    .action-text {
      .action-title {
        font-size: 15px;
        font-weight: 600;
        color: #1e293b;
        margin-bottom: 4px;
      }
      .action-desc {
        font-size: 12px;
        color: #64748b;
      }
    }
  }
}
</style>