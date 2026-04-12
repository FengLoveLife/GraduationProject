<template>
  <div class="inventory-dashboard" v-loading="loading">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">库存预警与监控中心</h2>
        <p class="subtitle">实时掌控全品类商品库存健康度，智能捕捉缺货风险</p>
      </div>
      <div class="header-right">
        <el-button
          :loading="exportLoading"
          :icon="Download"
          @click="handleExport"
          class="export-btn"
        >
          导出紧急补货单
        </el-button>
      </div>
    </div>

    <!-- KPI 卡片区 -->
    <el-row :gutter="20" class="kpi-section">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">在售商品总数</div>
              <div class="kpi-value">{{ kpiData.totalSku }}</div>
            </div>
            <div class="icon-wrapper icon-wrapper-blue">
              <el-icon><Box /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card clickable-card" @click="openDetail('warning')">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">库存告急 SKU</div>
              <div class="kpi-value warning">{{ kpiData.warningSku }}</div>
            </div>
            <div class="icon-wrapper icon-wrapper-warning">
              <el-icon><Warning /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card clickable-card" @click="openDetail('soldOut')">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">已售罄 SKU</div>
              <div class="kpi-value danger">{{ kpiData.soldOutSku }}</div>
            </div>
            <div class="icon-wrapper icon-wrapper-danger">
              <el-icon><CircleClose /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-card-content">
            <div class="kpi-text">
              <div class="kpi-label">库存总货值</div>
              <div class="kpi-value">{{ formatMoney(kpiData.totalValue) }}</div>
            </div>
            <div class="icon-wrapper icon-wrapper-success">
              <el-icon><Money /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <el-icon style="color: #67C23A; margin-right: 6px;"><PieChart /></el-icon>
              <span>库存健康度分布</span>
            </div>
          </template>
          <div ref="healthChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <el-icon style="color: #E6A23C; margin-right: 6px;"><TrendCharts /></el-icon>
              <span>各一级分类缺货排行 (SKU数)</span>
            </div>
          </template>
          <div ref="categoryWarningChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预警详情弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'warning' ? '库存告急商品' : '已售罄商品'"
      width="860px"
      align-center
      destroy-on-close
      class="warning-dialog"
    >
      <div class="dialog-sub-header">
        <el-tag :type="dialogType === 'warning' ? 'warning' : 'danger'" effect="dark" size="large">
          {{ dialogType === 'warning' ? '⚠ 库存告急' : '✕ 已售罄' }}
        </el-tag>
        <span class="count-badge">共 {{ detailList.length }} 种商品需要关注</span>
      </div>

      <div v-loading="listLoading" class="dialog-body">
        <div v-if="detailList.length > 0" class="product-grid">
          <div v-for="item in detailList" :key="item.id" class="product-card" :class="dialogType === 'soldOut' ? 'card-danger' : 'card-warning'">
            <!-- 状态角标 -->
            <div class="card-badge" :class="dialogType === 'soldOut' ? 'badge-danger' : 'badge-warning'">
              {{ dialogType === 'soldOut' ? '售罄' : '告急' }}
            </div>
            <!-- 商品图片 -->
            <div class="card-img-wrap">
              <el-image :src="item.imageUrl" class="card-img" fit="cover">
                <template #error>
                  <div class="card-img-error"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
            </div>
            <!-- 商品信息 -->
            <div class="card-body">
              <div class="card-name" :title="item.name">{{ item.name }}</div>
              <div class="card-meta">
                <span class="card-code">{{ item.productCode }}</span>
                <el-tag size="small" type="info" plain>{{ item.categoryName }}</el-tag>
              </div>
              <!-- 库存进度条 -->
              <div class="stock-bar-wrap">
                <div class="stock-bar-track">
                  <div
                    class="stock-bar-fill"
                    :class="dialogType === 'soldOut' ? 'fill-danger' : 'fill-warning'"
                    :style="{ width: item.safetyStock > 0 ? Math.min(100, (item.stock / item.safetyStock) * 100) + '%' : '0%' }"
                  ></div>
                </div>
                <div class="stock-nums">
                  <span>当前 <strong>{{ item.stock }}</strong></span>
                  <span>安全 {{ item.safetyStock }}</span>
                </div>
              </div>
              <!-- 缺口 -->
              <div class="card-gap" :class="dialogType === 'soldOut' ? 'gap-danger' : 'gap-warning'">
                缺口 ▲ {{ Math.abs(item.gapAmount) }}
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无相关商品" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import * as echarts from 'echarts'
import request from '../../../utils/request'
import { ElMessage } from 'element-plus'
import { Box, Warning, CircleClose, Money, Picture, Download, PieChart, TrendCharts } from '@element-plus/icons-vue'

// --- 状态定义 ---
const loading = ref(false)
const exportLoading = ref(false)
const kpiData = reactive({
  totalSku: 0,
  warningSku: 0,
  soldOutSku: 0,
  totalValue: 0
})

// 详情弹窗相关
const dialogVisible = ref(false)
const dialogType = ref('')
const listLoading = ref(false)
const detailList = ref([])

const healthChartRef = ref(null)
const categoryWarningChartRef = ref(null)
let healthChart = null
let categoryWarningChart = null

// --- 格式化方法 ---
const formatMoney = (val) => {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY'
  }).format(val || 0)
}

// --- 接口调用 ---
const fetchDashboardData = async () => {
  loading.value = true
  try {
    const res = await request.get('/inventory/dashboard')
    const data = res.data
    
    // 更新 KPI
    Object.assign(kpiData, data.kpi)
    
    // 初始化图表
    initHealthChart(data.healthData)
    initCategoryWarningChart(data.categoryWarningData)
  } catch (error) {
    console.error('获取库存看板数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 打开预警详情
const openDetail = async (type) => {
  dialogType.value = type
  dialogVisible.value = true
  listLoading.value = true
  detailList.value = []
  
  try {
    const res = await request.get(`/inventory/warning-list?type=${type}`)
    detailList.value = res.data || []
  } catch (error) {
    console.error('获取预警明细失败:', error)
  } finally {
    listLoading.value = false
  }
}

// 导出紧急补货单
const handleExport = async () => {
  exportLoading.value = true
  try {
    const res = await request.get('/inventory/export-warning', {
      responseType: 'blob'
    })
    
    // 从响应中获取 Blob 数据 (根据 request.js 拦截器，res 可能是响应对象)
    const blob = res.data || res
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    
    // 尝试从 header 获取文件名，否则使用默认名
    const contentDisposition = res.headers ? res.headers['content-disposition'] : ''
    let fileName = `紧急补货清单_${new Date().getTime()}.xlsx`
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename\*=utf-8''(.+)$/) || contentDisposition.match(/filename=(.+)$/)
      if (fileNameMatch && fileNameMatch[1]) {
        fileName = decodeURIComponent(fileNameMatch[1])
      }
    }
    
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// --- 图表逻辑 ---
const initHealthChart = (data) => {
  if (!healthChartRef.value) return
  if (!healthChart) {
    healthChart = echarts.init(healthChartRef.value)
  }
  
  const total = data.reduce((s, i) => s + (Number(i.value) || 0), 0)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const pct = total > 0 ? ((params.value / total) * 100).toFixed(1) : 0
        return `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${params.color};margin-right:6px;"></span>${params.name}：${params.value} 种（${pct}%）`
      }
    },
    legend: { bottom: '2%', left: 'center', textStyle: { color: '#606266' } },
    series: [{
      name: '库存健康度',
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      emphasis: { scale: true, scaleSize: 6, itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } },
      labelLine: { show: false },
      data: data.map(item => {
        let color = '#67C23A'
        if (item.name === '库存告急') color = '#E6A23C'
        if (item.name === '已经售罄') color = '#F56C6C'
        return { value: item.value, name: item.name, itemStyle: { color } }
      })
    }],
    graphic: {
      type: 'group', left: '50%', top: '36%',
      children: [
        { type: 'text', style: { text: '在售商品', textAlign: 'center', fill: '#909399', font: '13px sans-serif' }, top: -10 },
        { type: 'text', style: { text: String(total), textAlign: 'center', fill: '#1e293b', font: 'bold 28px sans-serif' }, top: 8 }
      ]
    }
  }
  healthChart.setOption(option)
}

const initCategoryWarningChart = (data) => {
  if (!categoryWarningChartRef.value) return
  if (!categoryWarningChart) {
    categoryWarningChart = echarts.init(categoryWarningChartRef.value)
  }
  
  // 倒序排列，让数值大的在上面
  const sortedData = [...data].reverse()
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01]
    },
    yAxis: {
      type: 'category',
      data: sortedData.map(item => item.name)
    },
    series: [
      {
        name: '缺货 SKU 数',
        type: 'bar',
        data: sortedData.map(item => item.value),
        itemStyle: {
          color: '#ffce00',
          borderRadius: [0, 4, 4, 0]
        },
        barWidth: '60%'
      }
    ]
  }
  categoryWarningChart.setOption(option)
}

const handleResize = () => {
  healthChart && healthChart.resize()
  categoryWarningChart && categoryWarningChart.resize()
}

onMounted(() => {
  fetchDashboardData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  healthChart && healthChart.dispose()
  categoryWarningChart && categoryWarningChart.dispose()
})
</script>

<style scoped lang="scss">
.inventory-dashboard {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 100px);

  .page-header {
    background: linear-gradient(135deg, #e67e00 0%, #ff9800 45%, #ffce00 100%);
    border-radius: 12px;
    padding: 24px 28px;
    margin-bottom: 24px;
    box-shadow: 0 4px 20px rgba(230, 126, 0, 0.28);
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #fff;

    .title {
      font-size: 22px;
      font-weight: 700;
      margin: 0 0 6px 0;
      color: #fff;
    }
    .subtitle {
      font-size: 13px;
      margin: 0;
      color: rgba(255, 255, 255, 0.85);
    }

    .export-btn {
      font-weight: bold;
      border-radius: 10px;
      border: 2px solid rgba(255,255,255,0.7);
      color: #fff;
      background: rgba(255,255,255,0.18);
      transition: all 0.3s;
      &:hover {
        background: rgba(255,255,255,0.32);
      }
    }
  }

  .kpi-section {
    margin-bottom: 24px;
    
    .kpi-card {
      border-radius: 12px;
      border: none;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
      transition: all 0.3s ease;

      &.clickable-card {
        cursor: pointer;
        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
        }
      }

      .kpi-card-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 5px;

        .kpi-text {
          .kpi-label {
            font-size: 14px;
            color: #909399;
            margin-bottom: 8px;
          }
          .kpi-value {
            font-size: 28px;
            font-weight: bold;
            color: #303133;
            &.warning { color: #e6a23c; }
            &.danger { color: #f56c6c; }
          }
        }

        .icon-wrapper {
          width: 56px;
          height: 56px;
          border-radius: 12px;
          display: flex;
          justify-content: center;
          align-items: center;
          font-size: 24px;

          &.icon-wrapper-blue { background-color: #e6f1fc; color: #409eff; }
          &.icon-wrapper-warning { background-color: #fdf6ec; color: #e6a23c; }
          &.icon-wrapper-danger { background-color: #fef0f0; color: #f56c6c; }
          &.icon-wrapper-success { background-color: #f0f9eb; color: #67c23a; }
        }
      }
    }
  }

  .chart-section {
    .chart-card {
      border-radius: 12px;
      border: none;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
      
      :deep(.el-card__header) {
        border-bottom: 1px solid #f1f5f9;
        padding: 16px 20px;
        span {
          font-size: 16px;
          font-weight: 700;
          color: #334155;
        }
      }

      .chart-box {
        height: 300px;
        width: 100%;
      }
    }
  }

  // 预警抽屉样式
  .warning-drawer {
    :deep(.el-drawer__header) {
      margin-bottom: 0;
      padding: 20px 24px;
      border-bottom: 1px solid #f1f5f9;
      span {
        font-weight: 800;
        color: #1e293b;
        font-size: 18px;
      }
    }
    
    .drawer-content {
      height: 100%;
      overflow-y: auto;
      
      .product-list-item {
        display: flex;
        align-items: center;
        padding: 16px 24px;
        border-bottom: 1px solid #f1f5f9;
        transition: background-color 0.2s;
        
        &:hover {
          background-color: #f8fafc;
        }
        
        .product-img {
          width: 64px;
          height: 64px;
          border-radius: 10px;
          flex-shrink: 0;
          box-shadow: 0 2px 8px rgba(0,0,0,0.05);
          
          .image-slot {
            display: flex;
            justify-content: center;
            align-items: center;
            width: 100%;
            height: 100%;
            background: #f1f5f9;
            color: #94a3b8;
            font-size: 24px;
          }
        }
        
        .product-info {
          flex: 1;
          padding-left: 16px;
          min-width: 0;
          
          .p-name {
            font-size: 15px;
            font-weight: 700;
            color: #334155;
            margin-bottom: 6px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          
          .p-meta {
            display: flex;
            align-items: center;
            gap: 8px;
            
            .p-code {
              font-size: 12px;
              color: #94a3b8;
              font-family: monospace;
            }
            
            .p-tag {
              height: 20px;
              line-height: 18px;
              padding: 0 6px;
              border-radius: 4px;
            }
          }
        }
        
        .product-stock {
          text-align: right;
          flex-shrink: 0;
          
          .stock-label {
            font-size: 12px;
            color: #64748b;
            margin-bottom: 6px;
          }
          
          .gap-tag {
            font-weight: 700;
            border: none;
            padding: 0 10px;
          }
        }
      }
    }
  }

  .card-header {
    display: flex;
    align-items: center;
  }
}

/* 弹窗样式（非 scoped，使用深层选择器覆盖） */
.warning-dialog .dialog-sub-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.warning-dialog .count-badge {
  font-size: 13px;
  color: #606266;
}

.warning-dialog .dialog-body {
  min-height: 200px;
}

.warning-dialog .product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.warning-dialog .product-card {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.07);
  border: 1.5px solid #f0f0f0;
  transition: transform 0.2s, box-shadow 0.2s;
}

.warning-dialog .product-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.1);
}

.warning-dialog .product-card.card-warning { border-color: rgba(230, 162, 60, 0.3); }
.warning-dialog .product-card.card-danger  { border-color: rgba(245, 108, 108, 0.3); }

.warning-dialog .card-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  z-index: 1;
}

.warning-dialog .badge-warning { background: #E6A23C; }
.warning-dialog .badge-danger  { background: #F56C6C; }

.warning-dialog .card-img-wrap {
  width: 100%;
  height: 120px;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.warning-dialog .card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.warning-dialog .card-img-error {
  font-size: 32px;
  color: #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.warning-dialog .card-body {
  padding: 12px;
}

.warning-dialog .card-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.warning-dialog .card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.warning-dialog .card-code {
  font-size: 11px;
  color: #909399;
}

.warning-dialog .stock-bar-wrap {
  margin-bottom: 8px;
}

.warning-dialog .stock-bar-track {
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.warning-dialog .stock-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s;
}

.warning-dialog .fill-warning { background: #E6A23C; }
.warning-dialog .fill-danger  { background: #F56C6C; }

.warning-dialog .stock-nums {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #606266;
}

.warning-dialog .stock-nums strong {
  color: #303133;
}

.warning-dialog .card-gap {
  font-size: 12px;
  font-weight: 600;
  text-align: right;
  margin-top: 4px;
}

.warning-dialog .gap-warning { color: #E6A23C; }
.warning-dialog .gap-danger  { color: #F56C6C; }
</style>
