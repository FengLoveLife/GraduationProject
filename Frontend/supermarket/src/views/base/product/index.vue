<script setup>
import { onMounted, reactive, ref, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, EditPen, Delete, Warning, Picture, Download, QuestionFilled, TrendCharts, Goods, Money, DataAnalysis, Top, Bottom } from '@element-plus/icons-vue'
import request from '../../../utils/request'
import * as echarts from 'echarts'

// --- 状态定义 ---
const loading = ref(false)
const productList = ref([])
const total = ref(0)
const categoryOptions = ref([])

// 导出相关
const exportVisible = ref(false)
const exportLoading = ref(false)
const selectedFields = ref(['productCode', 'name', 'stock', 'salePrice'])
const fieldOptions = [
  { label: '商品编码', value: 'productCode', disabled: true },
  { label: '商品名称', value: 'name' },
  { label: '所属分类', value: 'categoryId' },
  { label: '规格', value: 'specification' },
  { label: '单位', value: 'unit' },
  { label: '进货价', value: 'purchasePrice' },
  { label: '零售价', value: 'salePrice' },
  { label: '当前库存', value: 'stock' },
  { label: '安全库存', value: 'safetyStock' },
  { label: '状态', value: 'status' }
]

// 查询参数
const queryParams = reactive({
  page: 1,
  pageSize: 5,
  keyword: '',
  categoryId: null,
  status: null
})

// 级联选择器配置
const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null,
  categoryId: null,
  productCode: '',
  name: '',
  specification: '',
  unit: '',
  imageUrl: '',
  purchasePrice: 0,
  salePrice: 0,
  stock: 0,
  safetyStock: 0,
  status: 1
})

const rules = {
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入计价单位', trigger: 'blur' }],
  purchasePrice: [{ required: true, message: '请输入进货价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入零售价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入初始库存', trigger: 'blur' }]
}

// ========== 销售概况相关 ==========
const summaryVisible = ref(false)
const summaryLoading = ref(false)
const summaryData = ref(null)
const currentProduct = ref(null)
const trendChartRef = ref(null)
let trendChart = null

// --- 接口调用 ---

function getCategoryName(id, tree = categoryOptions.value) {
  if (!id || !tree || tree.length === 0) return '未知分类'
  for (const node of tree) {
    if (node.id === id) return node.name
    if (node.children && node.children.length > 0) {
      const foundName = getCategoryName(id, node.children)
      if (foundName !== '未知分类') return foundName
    }
  }
  return '未知分类'
}

async function fetchCategoryTree() {
  try {
    const res = await request.get('/category/tree')
    categoryOptions.value = res.data || []
  } catch (e) {
    console.error('获取分类树失败', e)
  }
}

async function fetchProductPage() {
  loading.value = true
  try {
    const res = await request.get('/product/page', { params: queryParams })
    const data = res.data
    productList.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('获取商品列表失败', e)
  } finally {
    loading.value = false
  }
}

// --- 交互逻辑 ---

function handleQuery() {
  queryParams.page = 1
  fetchProductPage()
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.categoryId = null
  queryParams.status = null
  queryParams.page = 1
  fetchProductPage()
}

function handleSizeChange(val) {
  queryParams.pageSize = val
  fetchProductPage()
}

function handleCurrentChange(val) {
  queryParams.page = val
  fetchProductPage()
}

// 新增
function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑
async function handleEdit(row) {
  isEdit.value = true
  resetForm()
  try {
    const res = await request.get(`/product/${row.id}`)
    const data = res.data
    Object.assign(formData, data)
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取商品详情失败')
  }
}

// 删除
function handleDelete(row) {
  ElMessageBox.confirm(`确定要永久删除商品【${row.name}】吗？此操作不可恢复！`, '风险提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      await request.delete(`/product/${row.id}`)
      ElMessage.success('删除成功')
      fetchProductPage()
    } catch (e) {
      // 错误已由拦截器统一处理，此处无需重复弹窗
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

// 重置表单
function resetForm() {
  if (formRef.value) formRef.value.resetFields()
  Object.assign(formData, {
    id: null,
    categoryId: null,
    productCode: '',
    name: '',
    specification: '',
    unit: '',
    imageUrl: '',
    purchasePrice: 0,
    salePrice: 0,
    stock: 0,
    safetyStock: 0,
    status: 1
  })
}

// 图片上传校验
function beforeUpload(file) {
  const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isJPGorPNG) {
    ElMessage.error('上传图片只能是 JPG 或 PNG 格式!')
  }
  if (!isLt5M) {
    ElMessage.error('上传图片大小不能超过 5MB!')
  }
  return isJPGorPNG && isLt5M
}

// 自定义图片上传
async function uploadImage(options) {
  const fd = new FormData()
  fd.append('file', options.file)
  try {
    const res = await request.post('/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    formData.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

// 提交表单
async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          await request.put('/product/', formData)
          ElMessage.success('修改成功')
        } else {
          await request.post('/product/', formData)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        fetchProductPage()
      } catch (e) {
        // 错误提示由拦截器完成
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 导出 Excel
async function handleExport() {
  exportLoading.value = true
  try {
    const fields = selectedFields.value.join(',')
    const res = await request.get('/product/export', {
      params: {
        ...queryParams,
        fields
      },
      responseType: 'blob'
    })

    const blob = res.data
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = `商品数据导出_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(link.href)

    ElMessage.success('导出成功')
    exportVisible.value = false
  } catch (e) {
    console.error('导出失败', e)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// ========== 销售概况 ==========

async function handleViewSummary(row) {
  currentProduct.value = row
  summaryVisible.value = true
  summaryLoading.value = true
  summaryData.value = null

  try {
    const res = await request.get(`/product/${row.id}/sales-summary`)
    summaryData.value = res.data

    // 渲染趋势图
    nextTick(() => {
      renderTrendChart()
    })
  } catch (e) {
    console.error('获取销售概况失败', e)
    ElMessage.error('获取销售概况失败')
  } finally {
    summaryLoading.value = false
  }
}

function renderTrendChart() {
  if (!trendChartRef.value || !summaryData.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0',
      borderWidth: 1,
      textStyle: { color: '#333', fontSize: 12 },
      formatter: (params) => {
        const date = params[0].axisValue
        const qty = params[0].value
        const amount = params[1]?.value || 0
        return `<div style="font-weight:600;margin-bottom:4px;">${date}</div>
                <div>销量：<span style="color:#409EFF;font-weight:600;">${qty}</span> 件</div>
                <div>销售额：<span style="color:#67C23A;font-weight:600;">¥${amount.toFixed(2)}</span></div>`
      }
    },
    legend: {
      data: ['销量', '销售额'],
      top: 0,
      right: 0,
      textStyle: { fontSize: 11 }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 35,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: summaryData.value.recentDates || [],
      axisLabel: {
        fontSize: 10,
        color: '#909399',
        interval: 4
      },
      axisLine: { lineStyle: { color: '#dcdfe6' } }
    },
    yAxis: [
      {
        type: 'value',
        name: '销量',
        nameTextStyle: { fontSize: 10, color: '#909399' },
        axisLabel: { fontSize: 10, color: '#909399' },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '销售额',
        nameTextStyle: { fontSize: 10, color: '#909399' },
        axisLabel: { fontSize: 10, color: '#909399', formatter: '¥{value}' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '销量',
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 2, color: '#409EFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.25)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
          ])
        },
        data: summaryData.value.recentQuantities || []
      },
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        symbol: 'none',
        yAxisIndex: 1,
        lineStyle: { width: 2, color: '#67C23A', type: 'dashed' },
        data: summaryData.value.recentAmounts || []
      }
    ]
  }

  trendChart.setOption(option)
}

function formatNumber(num) {
  if (num === null || num === undefined) return '0'
  return Number(num).toLocaleString('zh-CN')
}

function formatPercent(rate) {
  if (rate === null || rate === undefined) return '0.00%'
  return (Number(rate) * 100).toFixed(2) + '%'
}

function formatDate(date) {
  if (!date) return '-'
  return date
}

function closeSummaryDialog() {
  summaryVisible.value = false
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
}

onMounted(() => {
  fetchCategoryTree()
  fetchProductPage()
})

onUnmounted(() => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<template>
  <div class="product-container">
    <!-- 顶部搜索区 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索商品名称/编码"
            clearable
            @keyup.enter="handleQuery"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-cascader
            v-model="queryParams.categoryId"
            :options="categoryOptions"
            :props="cascaderProps"
            placeholder="请选择分类"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-yellow" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格区 -->
    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" class="btn-yellow" :icon="Plus" @click="handleAdd">新增商品</el-button>
        <el-button type="success" :icon="Download" @click="exportVisible = true">导出 Excel</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="productList"
        border
        stripe
        style="width: 100%"
        class="product-table"
      >
        <el-table-column label="图片" width="80" align="center">
          <template #default="{ row }">
            <el-image
              class="product-img"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
              fit="cover"
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>

        <el-table-column label="商品信息" min-width="200">
          <template #default="{ row }">
            <div class="info-cell">
              <div class="p-name">{{ row.name }}</div>
              <div class="p-code">{{ row.productCode }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="所属分类" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain" round class="category-tag">
              {{ getCategoryName(row.categoryId) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="规格 / 单位" width="150" align="center">
          <template #default="{ row }">
            <span class="spec-text">{{ row.specification || '-' }}</span>
            <span class="unit-text"> / {{ row.unit }}</span>
          </template>
        </el-table-column>

        <el-table-column label="进货价" width="110" align="right">
          <template #default="{ row }">
            <span class="price-text">￥{{ row.purchasePrice?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="零售价" width="110" align="right">
          <template #default="{ row }">
            <span class="price-text bold">￥{{ row.salePrice?.toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="库存 / 安全值" width="150" align="center">
          <template #default="{ row }">
            <div :class="['stock-cell', { 'is-warning': row.stock < row.safetyStock }]">
              <span class="stock-num">{{ row.stock }}</span>
              <span class="stock-divider">/</span>
              <span class="safety-num">{{ row.safetyStock }}</span>
              <el-icon v-if="row.stock < row.safetyStock" class="warning-icon"><Warning /></el-icon>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" :icon="TrendCharts" @click="handleViewSummary(row)">销售概况</el-button>
            <el-button link type="primary" :icon="EditPen" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="720px"
      destroy-on-close
      :close-on-click-modal="false"
      class="product-dialog"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        label-position="right"
      >
        <el-row :gutter="20">
          <!-- 左列 -->
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-cascader
                v-model="formData.categoryId"
                :options="categoryOptions"
                :props="cascaderProps"
                placeholder="请选择分类"
                clearable
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入商品名称" clearable />
            </el-form-item>
            <el-form-item label="规格" prop="specification">
              <el-input v-model="formData.specification" placeholder="如：500ml" clearable />
            </el-form-item>
            <el-form-item label="进货价" prop="purchasePrice">
              <el-input-number
                v-model="formData.purchasePrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="当前库存" prop="stock">
              <div class="stock-input-wrapper">
                <el-input-number
                  v-model="formData.stock"
                  :min="0"
                  controls-position="right"
                  style="width: 100%"
                  :disabled="isEdit"
                />
                <el-tooltip
                  v-if="isEdit"
                  content="库存金额涉及账目安全，编辑商品时不可更改。如需调整，请前往【库存预警与监控 -> 库存变动流水】进行手工盘点操作。"
                  placement="top"
                >
                  <el-icon class="stock-tip-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
            </el-form-item>
          </el-col>

          <!-- 右列 -->
          <el-col :span="12">
            <el-form-item label="商品编码" prop="productCode">
              <el-input v-model="formData.productCode" placeholder="请输入系统唯一编码" clearable />
            </el-form-item>
            <el-form-item label="计价单位" prop="unit">
              <el-input v-model="formData.unit" placeholder="如：瓶 / 袋" clearable />
            </el-form-item>
            <el-form-item label="商品图片" prop="imageUrl">
              <el-upload
                class="product-uploader"
                action="#"
                :show-file-list="false"
                :http-request="uploadImage"
                :before-upload="beforeUpload"
              >
                <img v-if="formData.imageUrl" :src="formData.imageUrl" class="uploaded-img" />
                <el-icon v-else class="uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="零售价" prop="salePrice">
              <el-input-number
                v-model="formData.salePrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="安全库存" prop="safetyStock">
              <el-input-number
                v-model="formData.safetyStock"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <!-- 底部满宽 -->
          <el-col :span="24">
            <el-form-item label="商品状态" prop="status">
              <el-switch
                v-model="formData.status"
                :active-value="1"
                :inactive-value="0"
                active-text="上架"
                inactive-text="下架"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" class="btn-yellow" :loading="submitLoading" @click="submitForm">
            确 定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导出配置弹窗 -->
    <el-dialog
      v-model="exportVisible"
      title="选择导出字段"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div class="export-options">
        <el-checkbox-group v-model="selectedFields">
          <el-row :gutter="20">
            <el-col :span="8" v-for="opt in fieldOptions" :key="opt.value" style="margin-bottom: 12px;">
              <el-checkbox :label="opt.value" :disabled="opt.disabled">
                {{ opt.label }}
              </el-checkbox>
            </el-col>
          </el-row>
        </el-checkbox-group>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="exportVisible = false">取 消</el-button>
          <el-button type="success" :loading="exportLoading" @click="handleExport">
            确认导出
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 销售概况弹窗 -->
    <el-dialog
      v-model="summaryVisible"
      :title="`销售概况 - ${currentProduct?.name || ''}`"
      width="800px"
      destroy-on-close
      :close-on-click-modal="false"
      class="summary-dialog"
      @closed="closeSummaryDialog"
    >
      <div v-loading="summaryLoading" class="summary-content">
        <template v-if="summaryData">
          <!-- 无销售数据提示 -->
          <div v-if="summaryData.totalQuantity === 0" class="no-data-tip">
            <el-empty description="该商品暂无销售记录" :image-size="120" />
          </div>

          <template v-else>
            <!-- KPI 卡片区 -->
            <div class="kpi-section">
              <div class="kpi-grid">
                <div class="kpi-card kpi-blue">
                  <div class="kpi-icon"><el-icon><Goods /></el-icon></div>
                  <div class="kpi-body">
                    <div class="kpi-value">{{ formatNumber(summaryData.totalQuantity) }}</div>
                    <div class="kpi-label">累计销量（件）</div>
                  </div>
                </div>
                <div class="kpi-card kpi-green">
                  <div class="kpi-icon"><el-icon><Money /></el-icon></div>
                  <div class="kpi-body">
                    <div class="kpi-value">¥{{ formatNumber(summaryData.totalAmount) }}</div>
                    <div class="kpi-label">累计销售额</div>
                  </div>
                </div>
                <div class="kpi-card kpi-orange">
                  <div class="kpi-icon"><el-icon><TrendCharts /></el-icon></div>
                  <div class="kpi-body">
                    <div class="kpi-value">¥{{ formatNumber(summaryData.totalProfit) }}</div>
                    <div class="kpi-label">累计毛利</div>
                  </div>
                </div>
                <div class="kpi-card kpi-purple">
                  <div class="kpi-icon"><el-icon><DataAnalysis /></el-icon></div>
                  <div class="kpi-body">
                    <div class="kpi-value">{{ formatPercent(summaryData.profitRate) }}</div>
                    <div class="kpi-label">毛利率</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 详细数据区 -->
            <div class="detail-section">
              <el-row :gutter="24">
                <el-col :span="12">
                  <div class="detail-card">
                    <div class="detail-title">销售时间维度</div>
                    <div class="detail-list">
                      <div class="detail-item">
                        <span class="label">首次销售</span>
                        <span class="value">{{ formatDate(summaryData.firstSaleDate) }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="label">最近销售</span>
                        <span class="value">{{ formatDate(summaryData.lastSaleDate) }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="label">销售天数</span>
                        <span class="value">{{ summaryData.saleDays }} 天</span>
                      </div>
                    </div>
                  </div>
                </el-col>
                <el-col :span="12">
                  <div class="detail-card">
                    <div class="detail-title">日均销售数据</div>
                    <div class="detail-list">
                      <div class="detail-item">
                        <span class="label">日均销量</span>
                        <span class="value">{{ summaryData.dailyAvgQuantity }} 件</span>
                      </div>
                      <div class="detail-item">
                        <span class="label">日均销售额</span>
                        <span class="value">¥{{ summaryData.dailyAvgAmount }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="label">平均售价</span>
                        <span class="value">¥{{ summaryData.avgUnitPrice }}</span>
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>

            <!-- 近30天对比 -->
            <div class="compare-section">
              <div class="compare-card">
                <div class="compare-header">
                  <span class="title">近30天表现</span>
                  <span class="tag" :class="summaryData.recentVsHistoryRate >= 0 ? 'up' : 'down'">
                    <el-icon v-if="summaryData.recentVsHistoryRate >= 0"><Top /></el-icon>
                    <el-icon v-else><Bottom /></el-icon>
                    {{ summaryData.recentVsHistoryRate >= 0 ? '+' : '' }}{{ formatPercent(summaryData.recentVsHistoryRate) }}
                    <span class="tag-label">vs 历史日均</span>
                  </span>
                </div>
                <div class="compare-body">
                  <div class="compare-item">
                    <span class="label">近30天销量</span>
                    <span class="value">{{ summaryData.recent30Quantity }} 件</span>
                  </div>
                  <div class="compare-item">
                    <span class="label">近30天销售额</span>
                    <span class="value">¥{{ formatNumber(summaryData.recent30Amount) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 趋势图 -->
            <div class="chart-section">
              <div class="chart-title">近30天销售趋势</div>
              <div ref="trendChartRef" class="chart-container"></div>
            </div>
          </template>
        </template>
      </div>
      <template #footer>
        <el-button @click="closeSummaryDialog">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
$yellow: #ffce00;

.product-container {
  padding: 16px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 110px);
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
  .search-form {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
  }
}

.table-card {
  border-radius: 8px;
  .table-toolbar {
    margin-bottom: 16px;
  }
}

.btn-yellow {
  background-color: $yellow !important;
  border-color: $yellow !important;
  color: #333 !important;
  font-weight: bold;
  &:hover {
    background-color: darken($yellow, 5%) !important;
    border-color: darken($yellow, 5%) !important;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 206, 0, 0.3);
  }
  &:active {
    transform: translateY(0);
  }
}

.info-cell {
  display: flex;
  flex-direction: column;
  .p-name {
    font-weight: bold;
    color: #303133;
    font-size: 14px;
    margin-bottom: 4px;
  }
  .p-code {
    color: #909399;
    font-size: 12px;
  }
}

.product-img {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  .image-slot {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background: #f5f7fa;
    color: #909399;
    font-size: 20px;
  }
}

.price-text {
  color: #606266;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  &.bold {
    font-weight: bold;
    color: #f56c6c;
  }
}

.stock-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 500;

  .stock-num {
    color: #303133;
  }
  .stock-divider {
    color: #dcdfe6;
    margin: 0 2px;
  }
  .safety-num {
    color: #909399;
    font-size: 12px;
  }

  &.is-warning {
    .stock-num {
      color: #f56c6c;
      font-weight: bold;
    }
    .warning-icon {
      color: #f56c6c;
      font-size: 16px;
      animation: blink 1.5s infinite;
    }
  }
}

@keyframes blink {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table__header) {
  th {
    background-color: #f8f9fb !important;
    color: #606266;
    font-weight: bold;
  }
}

.spec-text {
  color: #303133;
}
.unit-text {
  color: #909399;
  font-size: 12px;
}

.category-tag {
  font-weight: bold;
}

// 弹窗表单样式优化
.product-dialog {
  :deep(.el-dialog__body) {
    padding-top: 10px;
  }
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }
  :deep(.el-input__inner), :deep(.el-input-number__increase), :deep(.el-input-number__decrease) {
    border-radius: 4px;
  }
}

.export-options {
  padding: 10px 20px;
}

// 图片上传样式
.product-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 120px;
    height: 120px;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: border-color 0.3s;

    &:hover {
      border-color: $yellow;
    }
  }
}

.uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.uploaded-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.stock-input-wrapper {
  display: flex;
  align-items: center;
  width: 100%;

  .stock-tip-icon {
    margin-left: 8px;
    color: #909399;
    font-size: 16px;
    cursor: help;
  }
}

// ========== 销售概况弹窗样式 ==========
.summary-dialog {
  :deep(.el-dialog__body) {
    padding: 16px 20px;
    max-height: 70vh;
    overflow-y: auto;
  }
}

.summary-content {
  min-height: 200px;
}

.no-data-tip {
  padding: 40px 0;
}

// KPI 卡片
.kpi-section {
  margin-bottom: 20px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.kpi-card {
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;

  .kpi-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
  }

  .kpi-body {
    flex: 1;
  }

  .kpi-value {
    font-size: 20px;
    font-weight: 700;
    line-height: 1.3;
  }

  .kpi-label {
    font-size: 12px;
    opacity: 0.9;
    margin-top: 4px;
  }
}

.kpi-blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.kpi-green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.kpi-orange {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.kpi-purple {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

// 详细数据区
.detail-section {
  margin-bottom: 20px;
}

.detail-card {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px;
  height: 100%;
}

.detail-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.detail-list {
  .detail-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;

    &:not(:last-child) {
      border-bottom: 1px dashed #ebeef5;
    }

    .label {
      color: #909399;
      font-size: 13px;
    }

    .value {
      color: #303133;
      font-weight: 600;
      font-size: 14px;
    }
  }
}

// 对比区
.compare-section {
  margin-bottom: 20px;
}

.compare-card {
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
  border-radius: 12px;
  padding: 16px 20px;
}

.compare-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  .tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    border-radius: 16px;
    font-size: 13px;
    font-weight: 600;

    &.up {
      background: rgba(103, 194, 58, 0.1);
      color: #67c23a;
    }

    &.down {
      background: rgba(245, 108, 108, 0.1);
      color: #f56c6c;
    }

    .tag-label {
      font-weight: 400;
      color: #909399;
      margin-left: 4px;
    }
  }
}

.compare-body {
  display: flex;
  gap: 40px;

  .compare-item {
    display: flex;
    align-items: center;
    gap: 12px;

    .label {
      color: #606266;
      font-size: 13px;
    }

    .value {
      color: #303133;
      font-weight: 700;
      font-size: 15px;
    }
  }
}

// 趋势图
.chart-section {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  padding: 16px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.chart-container {
  height: 220px;
}

// 响应式
@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>