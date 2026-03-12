<template>
  <div class="inventory-dashboard" v-loading="loading">
    <!-- 头部标题 -->
    <div class="header-section">
      <div class="header-left">
        <h2 class="title">库存预警与监控中心</h2>
        <p class="subtitle">实时掌控全品类商品库存健康度，智能捕捉缺货风险</p>
      </div>
      <div class="header-right">
        <el-button 
          type="warning" 
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
              <span>各一级分类缺货排行 (SKU数)</span>
            </div>
          </template>
          <div ref="categoryWarningChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预警详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="500px"
      destroy-on-close
      class="warning-drawer"
    >
      <div v-loading="listLoading" class="drawer-content">
        <template v-if="detailList.length > 0">
          <div v-for="item in detailList" :key="item.id" class="product-list-item">
            <el-image :src="item.imageUrl" class="product-img" fit="cover">
              <template #error>
                <div class="image-slot"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <div class="product-info">
              <div class="p-name">{{ item.name }}</div>
              <div class="p-meta">
                <span class="p-code">{{ item.productCode }}</span>
                <el-tag size="small" type="info" plain class="p-tag">{{ item.categoryName }}</el-tag>
              </div>
            </div>
            <div class="product-stock">
              <div class="stock-label">当前: {{ item.stock }} / 安全: {{ item.safetyStock }}</div>
              <el-tag 
                :type="item.stock === 0 ? 'danger' : 'warning'" 
                size="small" 
                effect="dark"
                class="gap-tag"
              >
                缺口: {{ Math.abs(item.gapAmount) }}
              </el-tag>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无相关商品" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import * as echarts from 'echarts'
import request from '../../../utils/request'
import { Box, Warning, CircleClose, Money, Picture, Download } from '@element-plus/icons-vue'

// --- 状态定义 ---
const loading = ref(false)
const exportLoading = ref(false)
const kpiData = reactive({
  totalSku: 0,
  warningSku: 0,
  soldOutSku: 0,
  totalValue: 0
})

// 详情抽屉相关
const drawerVisible = ref(false)
const drawerTitle = ref('')
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
  drawerTitle.value = type === 'warning' ? '库存告急商品明细' : '已售罄商品明细'
  drawerVisible.value = true
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
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: '5%',
      left: 'center'
    },
    series: [
      {
        name: '库存健康度',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data.map(item => {
          let color = '#10b981'
          if (item.name === '库存告急') color = '#eab308'
          if (item.name === '已经售罄') color = '#ef4444'
          return {
            value: item.value,
            name: item.name,
            itemStyle: { color }
          }
        })
      }
    ]
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

    .export-btn {
      font-weight: bold;
      border-radius: 10px;
      padding: 12px 20px;
      box-shadow: 0 4px 12px rgba(234, 179, 8, 0.2);
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(234, 179, 8, 0.3);
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
}
</style>
