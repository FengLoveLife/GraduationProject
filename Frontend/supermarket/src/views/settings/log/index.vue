<template>
  <div class="log-page">
    <!-- 头部标题 -->
    <div class="header-section">
      <div class="header-left">
        <h2 class="title">操作日志</h2>
        <p class="subtitle">记录系统关键操作，安全可追溯</p>
      </div>
    </div>

    <!-- 筛选区域 -->
    <el-card shadow="hover" class="filter-card">
      <el-form :model="queryParams" inline class="filter-form">
        <el-form-item label="操作类型">
          <el-select
            v-model="queryParams.type"
            placeholder="全部类型"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            :shortcuts="dateShortcuts"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :icon="Search" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="hover" class="table-card">
      <el-table
        v-loading="loading"
        :data="logList"
        stripe
        class="log-table"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column label="操作时间" prop="createTime" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作人" prop="userName" width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-avatar">{{ row.userName?.slice(0, 1) }}</span>
              <span class="user-name">{{ row.userName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作类型" prop="operationTypeText" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.operationType)" effect="light" size="small">
              {{ row.operationTypeText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作内容" prop="operationDesc" min-width="200">
          <template #default="{ row }">
            <span class="desc-text">{{ row.operationDesc }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" prop="ipAddress" width="140">
          <template #default="{ row }">
            <span class="ip-text">{{ row.ipAddress || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getLogList } from '../../../api/log'

// 加载状态
const loading = ref(false)

// 查询参数
const queryParams = reactive({
  page: 1,
  size: 10,
  type: '',
  startDate: '',
  endDate: '',
})

// 日期范围
const dateRange = ref([])

// 日期快捷选项
const dateShortcuts = [
  {
    text: '今天',
    value: () => {
      const today = new Date()
      return [today, today]
    },
  },
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    },
  },
  {
    text: '最近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    },
  },
]

// 操作类型选项
const typeOptions = [
  { label: '登录', value: 'LOGIN' },
  { label: '密码', value: 'PASSWORD' },
  { label: '商品', value: 'PRODUCT' },
  { label: '销售', value: 'SALES' },
  { label: '库存', value: 'INVENTORY' },
]

// 日志列表
const logList = ref([])
const total = ref(0)

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

// 获取类型标签样式
const getTypeTagType = (type) => {
  const map = {
    LOGIN: 'success',
    PASSWORD: 'warning',
    PRODUCT: 'primary',
    SALES: '',
    INVENTORY: 'info',
  }
  return map[type] || ''
}

// 获取日志列表
const fetchLogList = async () => {
  loading.value = true
  try {
    // 处理日期参数
    if (dateRange.value && dateRange.value.length === 2) {
      queryParams.startDate = dateRange.value[0]
      queryParams.endDate = dateRange.value[1]
    } else {
      queryParams.startDate = ''
      queryParams.endDate = ''
    }

    const res = await getLogList(queryParams)
    logList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取日志列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  queryParams.page = 1
  fetchLogList()
}

// 重置
const handleReset = () => {
  queryParams.page = 1
  queryParams.type = ''
  dateRange.value = []
  fetchLogList()
}

// 分页
const handleSizeChange = () => {
  queryParams.page = 1
  fetchLogList()
}

const handleCurrentChange = () => {
  fetchLogList()
}

onMounted(() => {
  fetchLogList()
})
</script>

<style scoped lang="scss">
.log-page {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 100px);

  .header-section {
    margin-bottom: 24px;

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

  // 筛选卡片
  .filter-card {
    border-radius: 16px;
    border: none;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
    margin-bottom: 20px;

    .filter-form {
      :deep(.el-form-item) {
        margin-bottom: 0;
        margin-right: 16px;
      }

      :deep(.el-button--warning) {
        background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
        border: none;
        font-weight: 600;
        border-radius: 8px;

        &:hover {
          background: linear-gradient(135deg, #ffca28 0%, #ffa726 100%);
        }
      }

      :deep(.el-button--default) {
        border-radius: 8px;
        font-weight: 500;
      }
    }
  }

  // 数据表格卡片
  .table-card {
    border-radius: 16px;
    border: none;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);

    .log-table {
      --el-table-border-color: #f1f5f9;
      --el-table-row-hover-bg-color: #fefce8;

      .time-text {
        font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
        font-size: 13px;
        color: #64748b;
      }

      .user-cell {
        display: flex;
        align-items: center;
        gap: 8px;

        .user-avatar {
          width: 26px;
          height: 26px;
          border-radius: 8px;
          background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
          color: #fff;
          font-size: 12px;
          font-weight: 700;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .user-name {
          font-size: 14px;
          color: #334155;
          font-weight: 500;
        }
      }

      .desc-text {
        font-size: 14px;
        color: #334155;
      }

      .ip-text {
        font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
        font-size: 13px;
        color: #64748b;
        background: #f1f5f9;
        padding: 4px 8px;
        border-radius: 6px;
      }
    }
  }

  // 分页
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    padding: 20px 0 10px;

    :deep(.el-pagination) {
      .el-pagination__total,
      .el-pagination__sizes,
      .el-pagination__jump {
        font-weight: 500;
      }

      .el-pager li.is-active {
        background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
        color: #fff;
        font-weight: 700;
      }
    }
  }
}
</style>