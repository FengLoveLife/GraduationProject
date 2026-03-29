<template>
  <div class="dashboard">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-left">
        <h1 class="welcome-title">
          <span class="greeting">{{ greeting }}</span>
          <span class="name">，{{ userName }}</span>
        </h1>
        <p class="welcome-subtitle">{{ todayStr }} · {{ weekDay }} · 天气晴朗</p>
      </div>
      <div class="welcome-right">
        <div class="system-status">
          <span class="status-dot"></span>
          <span>系统运行正常</span>
        </div>
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
          <div class="kpi-trend up">
            <el-icon><Top /></el-icon>
            较昨日 +{{ kpiData.salesGrowth }}%
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
          <div class="kpi-trend up">
            <el-icon><Top /></el-icon>
            较昨日 +{{ kpiData.orderGrowth }}%
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

      <div class="kpi-card kpi-purple clickable" @click="goTo('/restocking/suggestion')">
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
            AI已生成建议
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
                <el-tag type="success" effect="plain" size="small">
                  <el-icon><Cpu /></el-icon>
                  LightGBM 预测
                </el-tag>
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
                  <span>今日品类销量占比</span>
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
  Money, Document, Warning, ShoppingCart, Top, Bell, Clock,
  TrendCharts, PieChart, Cpu, Operation, Upload, Box
} from '@element-plus/icons-vue'

const router = useRouter()

// 用户信息
const userName = ref('张店长')

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

// KPI 数据
const kpiData = reactive({
  todaySales: 8520,
  salesGrowth: 12.5,
  todayOrders: 42,
  orderGrowth: 5.3,
  stockWarning: 8,
  pendingPurchase: 3
})

// 图表引用
const salesChartRef = ref(null)
const categoryChartRef = ref(null)
let salesChart = null
let categoryChart = null

// 格式化数字
const formatNumber = (num) => {
  return num.toLocaleString('zh-CN')
}

// 跳转
const goTo = (path) => {
  router.push(path)
}

// 初始化销售趋势图
const initSalesChart = () => {
  if (!salesChartRef.value) return
  if (!salesChart) salesChart = echarts.init(salesChartRef.value)

  // 10天连续数据：前7天历史 + 后3天预测
  const dates = ['03-18', '03-19', '03-20', '03-21', '03-22', '03-23', '03-24', '03-25', '03-26', '03-27']
  // 历史数据（前7天）+ 预测数据（后3天）
  const salesData = [7200, 7800, 7500, 8200, 8900, 8100, 8520, 9100, 9600, 10200]
  // 标记哪些是预测数据
  const isPrediction = [false, false, false, false, false, false, false, true, true, true]

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' },
      formatter: function(params) {
        const p = params[0]
        const date = p.axisValue
        const value = p.value
        const prediction = isPrediction[p.dataIndex]
        return `<div style="font-weight:600;margin-bottom:4px">${date}</div>
                <div style="font-size:13px;color:#3B82F6;font-weight:700">
                  ¥${value.toLocaleString()}${prediction ? ' <span style="color:#67C23A;font-size:12px">(预测)</span>' : ''}
                </div>`
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
        data: salesData.map((v, i) => isPrediction[i] ? null : v),
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
        data: salesData.map((v, i) => isPrediction[i] ? v : null),
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

  const option = {
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter: '{b}: {c}件 ({d}%)'
    },
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
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: [
        { value: 156, name: '饮料', itemStyle: { color: '#409EFF' } },
        { value: 98, name: '休闲零食', itemStyle: { color: '#67C23A' } },
        { value: 72, name: '方便食品', itemStyle: { color: '#E6A23C' } },
        { value: 45, name: '乳制品', itemStyle: { color: '#F56C6C' } },
        { value: 38, name: '粮油调味', itemStyle: { color: '#909399' } }
      ]
    }]
  }
  categoryChart.setOption(option)
}

// 初始化图表
const initCharts = () => {
  nextTick(() => {
    initSalesChart()
    initCategoryChart()
  })
}

// 窗口resize
const handleResize = () => {
  salesChart?.resize()
  categoryChart?.resize()
}

onMounted(() => {
  initCharts()
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

  .system-status {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background: rgba(16, 185, 129, 0.1);
    border-radius: 20px;
    color: #10B981;
    font-size: 13px;
    font-weight: 500;

    .status-dot {
      width: 8px;
      height: 8px;
      background: #10B981;
      border-radius: 50%;
      animation: pulse 2s infinite;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
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
        color: #10B981;
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