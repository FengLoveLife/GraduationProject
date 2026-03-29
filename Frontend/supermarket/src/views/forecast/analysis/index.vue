<template>
  <div class="forecast-analysis">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">预测分析</h2>
        <p class="subtitle">查看预测准确率、误差分析、模型效果评估</p>
      </div>
      <div class="header-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
        />
      </div>
    </div>

    <!-- 准确率概览 -->
    <el-row :gutter="20" class="accuracy-row">
      <el-col :span="6">
        <div class="accuracy-card accuracy-blue">
          <div class="accuracy-value">{{ accuracyData.overall }}%</div>
          <div class="accuracy-label">整体准确率</div>
          <div class="accuracy-trend up">
            <el-icon><Top /></el-icon>
            较上周 +2.3%
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="accuracy-card accuracy-green">
          <div class="accuracy-value">{{ accuracyData.mape }}%</div>
          <div class="accuracy-label">MAPE 误差</div>
          <div class="accuracy-trend down">
            <el-icon><Bottom /></el-icon>
            误差降低 -1.2%
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="accuracy-card accuracy-orange">
          <div class="accuracy-value">{{ accuracyData.rmse }}</div>
          <div class="accuracy-label">RMSE 均方根误差</div>
          <div class="accuracy-trend">历史最佳: 4.2</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="accuracy-card accuracy-purple">
          <div class="accuracy-value">{{ accuracyData.coverage }}%</div>
          <div class="accuracy-label">预测覆盖率</div>
          <div class="accuracy-trend">{{ accuracyData.coveredProducts }}/{{ accuracyData.totalProducts }} 商品</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><DataLine /></el-icon>
                <span>预测 vs 实际销量对比</span>
              </div>
            </div>
          </template>
          <div ref="compareChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" style="color: #E6A23C"><PieChart /></el-icon>
                <span>误差分布</span>
              </div>
            </div>
          </template>
          <div ref="errorChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" style="color: #67C23A"><TrendCharts /></el-icon>
                <span>分类预测准确率</span>
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
                <el-icon class="header-icon" style="color: #409EFF"><Histogram /></el-icon>
                <span>预测准确率趋势</span>
              </div>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模型评估表格 -->
    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span>模型评估详情</span>
          <el-button type="primary" link>
            <el-icon><Download /></el-icon>
            导出报告
          </el-button>
        </div>
      </template>

      <el-table :data="modelMetrics" stripe style="width: 100%">
        <el-table-column prop="metric" label="评估指标" width="180" />
        <el-table-column prop="value" label="当前值" width="120" align="center">
          <template #default="{ row }">
            <span :class="getValueClass(row.status)">{{ row.value }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="目标值" width="120" align="center" />
        <el-table-column prop="previous" label="上周值" width="120" align="center" />
        <el-table-column label="变化趋势" width="150" align="center">
          <template #default="{ row }">
            <div class="trend-cell" :class="row.change > 0 ? 'up' : row.change < 0 ? 'down' : ''">
              <el-icon v-if="row.change > 0"><Top /></el-icon>
              <el-icon v-else-if="row.change < 0"><Bottom /></el-icon>
              <span>{{ row.change > 0 ? '+' : '' }}{{ row.change }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Top, Bottom, DataLine, PieChart, TrendCharts, Histogram, Download } from '@element-plus/icons-vue'

// 日期范围
const dateRange = ref([])

// 准确率数据
const accuracyData = reactive({
  overall: 87.6,
  mape: 12.4,
  rmse: 5.3,
  coverage: 94.5,
  coveredProducts: 176,
  totalProducts: 186
})

// 模型评估指标
const modelMetrics = ref([
  { metric: 'MAPE (平均绝对百分比误差)', value: '12.4%', target: '<20%', previous: '13.6%', change: -1.2, status: 'good', description: '误差越小越好，当前表现优秀' },
  { metric: 'RMSE (均方根误差)', value: '5.3', target: '<8', previous: '5.8', change: -0.5, status: 'good', description: '预测偏差范围可控' },
  { metric: 'MAE (平均绝对误差)', value: '4.2', target: '<6', previous: '4.8', change: -0.6, status: 'good', description: '平均预测偏差较小' },
  { metric: 'R² (决定系数)', value: '0.89', target: '>0.8', previous: '0.85', change: 4.0, status: 'good', description: '模型拟合度良好' },
  { metric: '命中率 (±10%范围内)', value: '78.3%', target: '>70%', previous: '75.2%', change: 3.1, status: 'good', description: '预测结果可信度高' },
  { metric: '高估率', value: '15.2%', target: '<20%', previous: '18.5%', change: -3.3, status: 'good', description: '模型略有保守倾向' },
])

// 图表引用
const compareChartRef = ref(null)
const errorChartRef = ref(null)
const categoryChartRef = ref(null)
const trendChartRef = ref(null)

let compareChart = null
let errorChart = null
let categoryChart = null
let trendChart = null

const getValueClass = (status) => {
  return status === 'good' ? 'value-good' : status === 'warning' ? 'value-warning' : 'value-danger'
}

const handleDateChange = () => {
  // 重新加载数据
}

// 初始化对比图
const initCompareChart = () => {
  if (!compareChartRef.value) return
  if (!compareChart) compareChart = echarts.init(compareChartRef.value)

  const dates = ['03-15', '03-16', '03-17', '03-18', '03-19', '03-20', '03-21', '03-22', '03-23', '03-24']
  const actual = [245, 268, 289, 302, 278, 256, 287, 310, 295, 278]
  const predicted = [252, 261, 295, 298, 285, 262, 280, 305, 290, 285]
  const upperBound = predicted.map(v => Math.round(v * 1.1))
  const lowerBound = predicted.map(v => Math.round(v * 0.9))

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['实际销量', '预测销量', '置信区间'], top: 10 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 60, containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      {
        name: '置信区间',
        type: 'line',
        data: upperBound,
        lineStyle: { opacity: 0 },
        stack: 'confidence',
        symbol: 'none',
        areaStyle: { color: 'rgba(103, 194, 83, 0.1)' }
      },
      {
        name: '置信区间',
        type: 'line',
        data: lowerBound,
        lineStyle: { opacity: 0 },
        stack: 'confidence',
        symbol: 'none',
        areaStyle: { color: 'rgba(103, 194, 83, 0.1)' }
      },
      {
        name: '实际销量',
        type: 'line',
        data: actual,
        smooth: true,
        lineStyle: { color: '#409EFF', width: 3 },
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '预测销量',
        type: 'line',
        data: predicted,
        smooth: true,
        lineStyle: { color: '#67C23A', width: 3, type: 'dashed' },
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
  compareChart.setOption(option)
}

// 初始化误差分布图
const initErrorChart = () => {
  if (!errorChartRef.value) return
  if (!errorChart) errorChart = echarts.init(errorChartRef.value)

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      data: [
        { value: 68, name: '误差<5%', itemStyle: { color: '#67C23A' } },
        { value: 22, name: '误差5-10%', itemStyle: { color: '#409EFF' } },
        { value: 7, name: '误差10-15%', itemStyle: { color: '#E6A23C' } },
        { value: 3, name: '误差>15%', itemStyle: { color: '#F56C6C' } }
      ],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } }
    }]
  }
  errorChart.setOption(option)
}

// 初始化分类准确率图
const initCategoryChart = () => {
  if (!categoryChartRef.value) return
  if (!categoryChart) categoryChart = echarts.init(categoryChartRef.value)

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: ['饮料', '休闲零食', '方便食品', '乳制品', '粮油调味', '肉制品'],
      axisLabel: { rotate: 0 }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%' }
    },
    series: [{
      type: 'bar',
      data: [92, 88, 85, 90, 82, 86],
      barWidth: 30,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: function(params) {
          const colors = ['#67C23A', '#409EFF', '#E6A23C', '#10B981', '#F59E0B', '#8B5CF6']
          return colors[params.dataIndex]
        }
      },
      label: { show: true, position: 'top', formatter: '{c}%' }
    }]
  }
  categoryChart.setOption(option)
}

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  const dates = ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'W7', 'W8']
  const accuracy = [82, 84, 83, 86, 85, 88, 87, 88]
  const target = 85

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 30, containLabel: true },
    xAxis: { type: 'category', data: dates },
    yAxis: {
      type: 'value',
      min: 75,
      max: 95,
      axisLabel: { formatter: '{value}%' }
    },
    series: [
      {
        type: 'line',
        data: accuracy,
        smooth: true,
        lineStyle: { color: '#409EFF', width: 3 },
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        markLine: {
          silent: true,
          data: [{ yAxis: target, name: '目标线', lineStyle: { color: '#67C23A', type: 'dashed' } }]
        }
      }
    ]
  }
  trendChart.setOption(option)
}

// 初始化所有图表
const initCharts = () => {
  nextTick(() => {
    initCompareChart()
    initErrorChart()
    initCategoryChart()
    initTrendChart()
  })
}

// 窗口大小变化处理
const handleResize = () => {
  compareChart?.resize()
  errorChart?.resize()
  categoryChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  // 初始化日期范围
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 30)
  dateRange.value = [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]

  initCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  compareChart?.dispose()
  errorChart?.dispose()
  categoryChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped lang="scss">
.forecast-analysis {
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

.accuracy-row {
  margin-bottom: 24px;

  .accuracy-card {
    padding: 24px;
    border-radius: 16px;
    text-align: center;
    color: #fff;

    .accuracy-value {
      font-size: 36px;
      font-weight: 800;
      margin-bottom: 8px;
    }
    .accuracy-label {
      font-size: 14px;
      opacity: 0.9;
      margin-bottom: 8px;
    }
    .accuracy-trend {
      font-size: 12px;
      opacity: 0.85;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;

      &.up { color: #fff; }
      &.down { color: #fff; }
    }

    &.accuracy-blue { background: linear-gradient(135deg, #3B82F6, #60A5FA); }
    &.accuracy-green { background: linear-gradient(135deg, #10B981, #34D399); }
    &.accuracy-orange { background: linear-gradient(135deg, #F59E0B, #FBBF24); }
    &.accuracy-purple { background: linear-gradient(135deg, #8B5CF6, #A78BFA); }
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

.table-card {
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

  .value-good { color: #10B981; font-weight: 700; }
  .value-warning { color: #F59E0B; font-weight: 700; }
  .value-danger { color: #EF4444; font-weight: 700; }

  .trend-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;

    &.up { color: #10B981; }
    &.down { color: #EF4444; }
  }
}
</style>