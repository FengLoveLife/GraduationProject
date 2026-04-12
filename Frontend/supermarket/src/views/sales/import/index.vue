<template>
  <div class="app-container">
    <!-- 顶部操作栏 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <div class="title">销售日结导入</div>
          <div class="subtitle">每日销售流水上报，系统自动扣减库存并生成财务台账</div>
        </div>
        <div class="actions">
          <el-button type="primary" icon="Upload" @click="importDialogVisible = true">
            导入日结单(Excel)
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 主体内容区 -->
    <el-row :gutter="20" class="main-content">
      <!-- 左侧：操作指南 -->
      <el-col :span="8">
        <el-card class="guide-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>导入指南</span>
            </div>
          </template>
          <div class="guide-body">
            <el-steps direction="vertical" :active="1" finish-status="success" class="custom-steps">
              <el-step title="下载模板" description="获取标准的 CSV/Excel 导入模板" />
              <el-step title="填报数据" description="按格式填写昨日 POS 机销售流水" />
              <el-step title="上传导入" description="点击右上角按钮上传，系统自动处理" />
            </el-steps>
            <div class="guide-footer">
              <el-button type="success" plain icon="Download" class="w-full" @click="downloadTemplate">
                下载标准模板 (CSV)
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：最近导入预览 -->
      <el-col :span="16">
        <el-card class="preview-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>最近生成的订单 (Top 5)</span>
              <el-button link icon="Refresh" :loading="tableLoading" @click="fetchRecentOrders">刷新</el-button>
            </div>
          </template>
          <el-table
            v-loading="tableLoading"
            :data="recentOrders"
            border
            stripe
            style="width: 100%"
            class="preview-table"
          >
            <el-table-column label="订单编号" prop="orderNo" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="order-no-text">{{ row.orderNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="销售时间" prop="saleTime" width="170" align="center" />
            <el-table-column label="总金额" width="120" align="right">
              <template #default="{ row }">
                <span class="amount-text">¥ {{ row.totalAmount?.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="件数" prop="totalQuantity" width="80" align="center" />
            <template #empty>
              <el-empty description="暂无最近导入记录" :image-size="100" />
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导入上传弹窗 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入销售日结单"
      width="480px"
      destroy-on-close
      :close-on-click-modal="false"
      class="upload-dialog"
    >
      <div class="upload-wrapper">
        <el-upload
          ref="uploadRef"
          class="upload-demo"
          drag
          action="#"
          accept=".xlsx, .xls"
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将 Excel 文件拖到此处，或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              请上传标准格式的销售日结单 (.xlsx / .xls)
            </div>
          </template>
        </el-upload>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取 消</el-button>
          <el-button
            type="primary"
            :loading="importLoading"
            @click="submitImport"
          >
            确认导入
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Download, Refresh, Upload } from '@element-plus/icons-vue'
import request from '../../../utils/request'

// --- 状态变量 ---
const importDialogVisible = ref(false)
const importLoading = ref(false)
const uploadRef = ref(null)
const selectedFile = ref(null)

const recentOrders = ref([])
const tableLoading = ref(false)

// --- 核心逻辑 ---

// 获取最近订单
const fetchRecentOrders = async () => {
  tableLoading.value = true
  try {
    const res = await request.get('/sales-order/page', {
      params: { page: 1, pageSize: 5 }
    })
    recentOrders.value = res.data.records || []
  } catch (error) {
    console.error('获取最近订单失败:', error)
  } finally {
    tableLoading.value = false
  }
}

// 下载模板 (纯前端生成)
const downloadTemplate = () => {
  const headers = "订单编号,商品编码,实际售价,销售数量,是否促销,支付方式,销售时间,收银机编号"
  const example = "ORD-20260310-001,SP1101,6.50,2,0,1,2026-03-10 09:00:00,POS-01"
  const csvContent = `${headers}\n${example}`
  
  // 添加 BOM 头防乱码
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  
  const link = document.createElement('a')
  link.href = url
  link.download = '销售日结单导入模板.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// --- 上传相关方法 ---

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleExceed = () => {
  ElMessage.warning('每次只能上传一个文件，请先移除当前文件')
}

const submitImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要导入的 Excel 文件')
    return
  }

  importLoading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)

  try {
    await request.post('/sales-order/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    ElMessage.success('销售数据导入成功')
    importDialogVisible.value = false
    selectedFile.value = null
    if (uploadRef.value) uploadRef.value.clearFiles()
    
    // 导入成功后立即刷新预览
    fetchRecentOrders()
  } catch (error) {
    const msg = error?.response?.data?.message || error?.message || '导入失败，请检查文件格式或联系管理员'
    ElMessage.error(msg)
  } finally {
    importLoading.value = false
  }
}

onMounted(() => {
  fetchRecentOrders()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.header-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section .title {
  font-size: 20px;
  font-weight: 800;
  color: #303133;
  margin-bottom: 6px;
}

.title-section .subtitle {
  font-size: 13px;
  color: #909399;
}

.guide-card, .preview-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  height: 420px; /* 固定高度保持对齐 */
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  color: #303133;
}

.guide-body {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 10px 0;
}

.custom-steps {
  height: 240px;
}

.w-full {
  width: 100%;
}

.order-no-text {
  font-family: monospace;
  color: #606266;
  font-weight: 600;
}

.amount-text {
  color: #303133;
  font-weight: bold;
}

/* 弹窗样式微调 */
.upload-wrapper {
  padding: 10px 0;
}
.upload-demo {
  width: 100%;
}
:deep(.el-upload-dragger) {
  width: 100%;
  border-radius: 8px;
}
</style>
