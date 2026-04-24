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

    <!-- 主体内容区：左右两个卡片高度一致 -->
    <el-row :gutter="20" class="main-content">
      <!-- 左侧：Excel导入指南 -->
      <el-col :span="6">
        <el-card class="guide-card" shadow="never">
          <template #header>
            <div class="card-header">导入指南</div>
          </template>
          <div class="guide-body">
            <el-steps direction="vertical" :active="1" finish-status="success" class="custom-steps">
              <el-step title="下载模板" description="获取标准导入模板" />
              <el-step title="填报数据" description="填写昨日销售流水" />
              <el-step title="上传导入" description="点击右上角按钮上传" />
            </el-steps>
            <div class="guide-footer">
              <el-button type="success" plain icon="Download" class="w-full" @click="downloadTemplate">
                下载标准模板 (CSV)
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：POS接口演示 -->
      <el-col :span="18">
        <el-card class="pos-card" shadow="never">
          <template #header>
            <div class="card-header-row">
              <div class="pos-title">
                <el-icon><Connection /></el-icon>
                <span>POS机接口演示工具</span>
              </div>
              <div class="pos-desc">模拟收银系统推送，验证接口对接能力</div>
            </div>
          </template>

          <!-- 顶部：POS编号、支付方式、搜索商品 横向排列 -->
          <div class="top-row">
            <div class="top-item">
              <span class="top-label">POS编号</span>
              <el-select v-model="simForm.posId" style="width: 120px">
                <el-option label="POS-01" value="POS-01" />
                <el-option label="POS-02" value="POS-02" />
                <el-option label="POS-03" value="POS-03" />
              </el-select>
            </div>
            <div class="top-item">
              <span class="top-label">支付方式</span>
              <el-radio-group v-model="simForm.paymentType" size="default">
                <el-radio-button :value="1">现金</el-radio-button>
                <el-radio-button :value="2">微信</el-radio-button>
                <el-radio-button :value="3">支付宝</el-radio-button>
              </el-radio-group>
            </div>
            <div class="top-item flex-1">
              <span class="top-label">搜索商品</span>
              <el-select
                v-model="selectedProductCode"
                filterable
                remote
                :remote-method="searchProducts"
                :loading="productLoading"
                placeholder="输入商品名称/编码"
                class="product-select"
                @change="handleAddProduct"
              >
                <el-option
                  v-for="p in productOptions"
                  :key="p.id"
                  :value="p.productCode"
                >
                  <div class="product-option">
                    <span class="opt-code">{{ p.productCode }}</span>
                    <span class="opt-name">{{ p.name }}</span>
                    <span class="opt-stock">库存 {{ p.stock }}</span>
                  </div>
                </el-option>
              </el-select>
            </div>
          </div>

          <!-- 购物车表格 -->
          <div class="cart-area">
            <div class="cart-head">
              <span>已选商品（{{ simForm.items.length }} 项）</span>
              <el-button v-if="simForm.items.length" link type="danger" @click="simForm.items = []">清空</el-button>
            </div>
            <el-table :data="simForm.items" empty-text="请搜索添加商品" size="small" stripe max-height="200">
              <el-table-column label="商品名称" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="prod-name">{{ row.productName }}</span>
                </template>
              </el-table-column>
              <el-table-column label="编码" prop="productCode" width="90" show-overflow-tooltip />
              <el-table-column label="售价(元)" width="100" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.unitPrice" :min="0.01" :precision="2" size="small" controls-position="right" class="price-input" />
                </template>
              </el-table-column>
              <el-table-column label="数量" width="90" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.quantity" :min="1" size="small" class="qty-input" />
                </template>
              </el-table-column>
              <el-table-column label="小计(元)" width="90" align="right">
                <template #default="{ row }">
                  <span class="amt">{{ (row.unitPrice * row.quantity).toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column width="50" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" :icon="Delete" @click="simForm.items.splice($index, 1)" />
                </template>
              </el-table-column>
            </el-table>
            <div class="cart-total">
              <span>订单合计：</span>
              <span class="amt-total">¥ {{ cartTotal.toFixed(2) }}</span>
            </div>
          </div>

          <!-- 推送按钮 -->
          <el-button type="primary" class="push-btn" :loading="pushing" :disabled="!simForm.items.length" @click="doSimulate">
            <el-icon><Promotion /></el-icon>&nbsp;模拟 POS 推送
          </el-button>

          <!-- 响应结果 -->
          <div v-if="lastSuccess" class="result-box">
            <el-icon class="result-icon"><CircleCheck /></el-icon>
            <div class="result-content">
              <div class="result-title">推送成功</div>
              <div class="result-detail">
                <span class="result-label">订单号：</span>
                <span class="result-value mono">{{ lastOrderNo }}</span>
              </div>
              <div class="result-detail">
                <span class="result-label">商品数：</span>
                <span class="result-value">{{ lastItemCount }} 件</span>
              </div>
              <div class="result-detail">
                <span class="result-label">总金额：</span>
                <span class="result-value amt">¥{{ lastAmount.toFixed(2) }}</span>
              </div>
              <div class="result-tip">可前往【销售流水查询】查看该订单</div>
            </div>
          </div>

          <div v-if="lastError" class="result-box error">
            <el-icon class="result-icon"><CircleClose /></el-icon>
            <div class="result-content">
              <div class="result-title">推送失败</div>
              <div class="result-detail error-msg">{{ lastErrorMsg }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导入上传弹窗 -->
    <el-dialog v-model="importDialogVisible" title="导入销售日结单" width="480px" destroy-on-close :close-on-click-modal="false">
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
          <div class="el-upload__text">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">请上传标准格式的销售日结单 (.xlsx / .xls)</div>
          </template>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Download, Upload, Connection, Promotion, Delete, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import request from '../../../utils/request'
import { simulatePosPush } from '../../../api/pos'

// --- 导入相关 ---
const importDialogVisible = ref(false)
const importLoading = ref(false)
const uploadRef = ref(null)
const selectedFile = ref(null)

// --- 模拟推送相关 ---
const simForm = reactive({
  posId: 'POS-01',
  paymentType: 2,
  items: [],
})
const cartTotal = computed(() =>
  simForm.items.reduce((s, i) => s + Number(i.unitPrice || 0) * Number(i.quantity || 0), 0)
)

const productOptions = ref([])
const productLoading = ref(false)
const selectedProductCode = ref(null)

const pushing = ref(false)
const lastSuccess = ref(false)
const lastError = ref(false)
const lastOrderNo = ref('')
const lastItemCount = ref(0)
const lastAmount = ref(0)
const lastErrorMsg = ref('')

// --- 商品搜索 ---
async function searchProducts(keyword) {
  productLoading.value = true
  try {
    const res = await request.get('/product/page', {
      params: { page: 1, pageSize: 30, keyword: keyword || '' }
    })
    productOptions.value = res.data?.records || []
  } catch (e) {
    console.error('商品搜索失败', e)
  } finally {
    productLoading.value = false
  }
}

function handleAddProduct(code) {
  const p = productOptions.value.find(x => x.productCode === code)
  if (!p) return
  const exist = simForm.items.find(i => i.productCode === code)
  if (exist) {
    exist.quantity += 1
  } else {
    simForm.items.push({
      productCode: p.productCode,
      productName: p.name,
      unitPrice: Number(p.salePrice),
      quantity: 1,
      isPromotion: 0,
    })
  }
  selectedProductCode.value = null
}

// --- 模拟推送 ---
async function doSimulate() {
  if (!simForm.items.length) {
    ElMessage.warning('请至少添加一件商品')
    return
  }
  const payload = {
    posId: simForm.posId,
    paymentType: simForm.paymentType,
    items: simForm.items.map(i => ({
      productCode: i.productCode,
      unitPrice: Number(i.unitPrice),
      quantity: Number(i.quantity),
      isPromotion: i.isPromotion || 0,
    })),
  }
  pushing.value = true
  lastSuccess.value = false
  lastError.value = false
  try {
    const res = await simulatePosPush(payload)
    if (res.code === 200) {
      lastSuccess.value = true
      lastOrderNo.value = res.data?.orderNo || ''
      lastItemCount.value = res.data?.itemCount || simForm.items.length
      lastAmount.value = res.data?.totalAmount || cartTotal.value
      ElMessage.success('推送成功！')
      simForm.items = []
    } else {
      lastError.value = true
      lastErrorMsg.value = res.msg || '推送失败'
    }
  } catch (e) {
    lastError.value = true
    lastErrorMsg.value = e.response?.data?.msg || e.message || '请求异常'
    ElMessage.error('请求异常')
  } finally {
    pushing.value = false
  }
}

// --- 导入相关方法 ---
const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleExceed = () => {
  ElMessage.warning('每次只能上传一个文件')
}

const submitImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  importLoading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  try {
    await request.post('/sales-order/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    selectedFile.value = null
    if (uploadRef.value) uploadRef.value.clearFiles()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

// --- 下载模板 ---
const downloadTemplate = () => {
  const headers = "订单编号,商品编码,实际售价,销售数量,是否促销,支付方式,销售时间,收银机编号"
  const example = "ORD-20260310-001,SP1101,6.50,2,0,1,2026-03-10 09:00:00,POS-01"
  const csvContent = `${headers}\n${example}`
  const blob = new Blob(['﻿' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '销售日结单导入模板.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

onMounted(() => {
  searchProducts('')
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

/* 左右卡片高度一致 */
.guide-card, .pos-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.guide-card, .guide-card :deep(.el-card__body) {
  height: 100%;
}

.guide-body {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.card-header {
  font-weight: 700;
  color: #303133;
}

.custom-steps {
  flex: 1;
}

.w-full {
  width: 100%;
}

.guide-footer {
  padding-top: 16px;
}

/* POS卡片 */
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pos-title {
  font-weight: 700;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.pos-desc {
  font-size: 13px;
  color: #909399;
}

/* 顶部横排 */
.top-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 12px;
}

.top-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-item.flex-1 {
  flex: 1;
}

.top-label {
  font-size: 13px;
  color: #475569;
  font-weight: 600;
}

/* 购物车 */
.cart-area {
  background: #fafbfc;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  padding: 10px 12px;
}

.cart-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #475569;
  font-weight: 600;
  padding: 0 0 8px;
}

.prod-name {
  font-weight: 600;
  color: #1f2937;
}

.amt {
  font-weight: 700;
  color: #059669;
}

.cart-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  padding: 10px 0 0;
  font-size: 13px;
  color: #475569;
}

.amt-total {
  font-size: 18px;
  font-weight: 800;
  color: #ef4444;
}

.push-btn {
  width: 100%;
  height: 44px;
  font-weight: 700;
  margin-top: 12px;
}

/* 响应结果 */
.result-box {
  margin-top: 16px;
  padding: 16px 20px;
  border-radius: 10px;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border: 1px solid #86efac;
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.result-box.error {
  background: linear-gradient(135deg, #fef2f2 0%, #fecaca 100%);
  border: 1px solid #fca5a5;
}

.result-icon {
  font-size: 28px;
  color: #22c55e;
}

.result-box.error .result-icon {
  color: #ef4444;
}

.result-content {
  flex: 1;
}

.result-title {
  font-size: 15px;
  font-weight: 700;
  color: #166534;
  margin-bottom: 8px;
}

.result-box.error .result-title {
  color: #b91c1c;
}

.result-detail {
  font-size: 13px;
  color: #475569;
  margin-bottom: 4px;
}

.result-label {
  color: #909399;
}

.result-value {
  color: #1f2937;
  font-weight: 600;
}

.mono {
  font-family: 'JetBrains Mono', Menlo, monospace;
}

.result-tip {
  font-size: 12px;
  color: #15803d;
  margin-top: 6px;
}

.error-msg {
  color: #dc2626;
}

/* 弹窗 */
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

/* 商品搜索下拉框样式 */
.product-select {
  width: 100%;
}

.product-option {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.opt-code {
  color: #6366f1;
  font-weight: 600;
  font-family: 'JetBrains Mono', Menlo, monospace;
  min-width: 70px;
}

.opt-name {
  color: #1f2937;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.opt-stock {
  color: #059669;
  font-size: 12px;
  background: #ecfdf5;
  padding: 2px 8px;
  border-radius: 4px;
}

/* 表格输入框样式 */
.price-input, .qty-input {
  width: 80px;
}

:deep(.price-input .el-input__inner),
:deep(.qty-input .el-input__inner) {
  text-align: center;
}
</style>