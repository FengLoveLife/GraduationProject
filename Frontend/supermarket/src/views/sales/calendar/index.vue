<template>
  <div class="app-container">
    <!-- 顶部操作栏 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <div class="title">日历因子管理</div>
          <div class="subtitle">维护影响销量的环境因子数据，用于AI预测模型</div>
        </div>
        <div class="actions">
          <el-button type="primary" icon="Plus" @click="handleInit">
            批量初始化
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 日历主体 -->
    <el-card class="calendar-card" shadow="never">
      <!-- 月份切换 -->
      <div class="calendar-header">
        <el-button link @click="prevMonth">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="month-title">{{ currentYear }}年{{ currentMonth }}月</span>
        <el-button link @click="nextMonth">
          <el-icon><ArrowRight /></el-icon>
        </el-button>
        <el-button size="small" @click="goToday" style="margin-left: 16px">今天</el-button>
      </div>

      <!-- 日历网格 -->
      <div class="calendar-grid" v-loading="loading">
        <!-- 星期标题 -->
        <div class="week-header">
          <div class="week-cell" v-for="day in weekDays" :key="day">{{ day }}</div>
        </div>

        <!-- 日期格子 -->
        <div class="date-grid">
          <div
            v-for="(cell, index) in calendarCells"
            :key="index"
            class="date-cell"
            :class="{
              'other-month': !cell.currentMonth,
              'is-weekend': cell.isWeekend,
              'is-holiday': cell.isHoliday,
              'is-today': cell.isToday
            }"
            @click="cell.currentMonth && handleCellClick(cell)"
          >
            <div class="date-number">{{ cell.day }}</div>
            <div class="tags" v-if="cell.currentMonth">
              <span class="tag weekend-tag" v-if="cell.isWeekend">周末</span>
              <span class="tag holiday-tag" v-if="cell.isHoliday">{{ cell.holidayName }}</span>
            </div>
            <div class="weather" v-if="cell.currentMonth && cell.weather">
              <span class="weather-icon">{{ cell.weatherIcon }}</span>
              <span class="weather-text">{{ cell.weather }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 图例 -->
      <div class="legend">
        <span class="legend-item"><span class="dot weekend"></span>周末（自动标记）</span>
        <span class="legend-item"><span class="dot holiday"></span>节假日（手动标记）</span>
        <span class="legend-item"><span class="dot weather-dot"></span>天气状态</span>
      </div>
    </el-card>

    <!-- 初始化弹窗 -->
    <el-dialog
      v-model="initDialogVisible"
      title="批量初始化日历数据"
      width="400px"
      destroy-on-close
    >
      <el-form :model="initForm" label-width="80px">
        <el-form-item label="年份">
          <el-input-number v-model="initForm.year" :min="2020" :max="2030" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="initForm.month" :min="1" :max="12" />
        </el-form-item>
      </el-form>
      <div class="init-tips">
        <el-alert type="info" :closable="false">
          <template #title>
            系统将自动生成该月所有日期，并自动标记周末。已存在的日期将被跳过。
          </template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="initDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="initLoading" @click="submitInit">确认生成</el-button>
      </template>
    </el-dialog>

    <!-- 编辑抽屉 -->
    <el-drawer
      v-model="editDrawerVisible"
      title="编辑日历因子"
      size="360px"
      destroy-on-close
    >
      <div class="edit-form" v-if="currentCell">
        <div class="edit-date">
          <el-icon><Calendar /></el-icon>
          <span>{{ currentCell.date }} ({{ currentCell.dayOfWeekText }})</span>
        </div>

        <el-divider />

        <el-form label-width="90px">
          <el-form-item label="是否周末">
            <el-tag :type="currentCell.isWeekend ? 'success' : 'info'">
              {{ currentCell.isWeekend ? '是（自动计算）' : '否' }}
            </el-tag>
          </el-form-item>

          <el-form-item label="是否节假日">
            <el-switch
              v-model="editForm.isHoliday"
              :active-value="1"
              :inactive-value="0"
            />
          </el-form-item>

          <el-form-item label="节假日名称" v-if="editForm.isHoliday">
            <el-input v-model="editForm.holidayName" placeholder="如：春节、国庆" />
          </el-form-item>

          <el-form-item label="天气">
            <el-select v-model="editForm.weather" placeholder="选择天气" clearable>
              <el-option label="☀️ 晴" value="晴" />
              <el-option label="☁️ 多云" value="多云" />
              <el-option label="🌧️ 小雨" value="小雨" />
              <el-option label="⛈️ 大雨" value="大雨" />
              <el-option label="❄️ 雪" value="雪" />
              <el-option label="🌡️ 炎热" value="炎热" />
            </el-select>
          </el-form-item>
        </el-form>

        <el-divider />

        <div class="edit-actions">
          <el-button @click="editDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Calendar } from '@element-plus/icons-vue'
import request from '../../../utils/request'

// 状态
const loading = ref(false)
const initLoading = ref(false)
const editLoading = ref(false)
const initDialogVisible = ref(false)
const editDrawerVisible = ref(false)

// 当前年月
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)

// 日历数据
const calendarData = ref([])

// 当前编辑的单元格
const currentCell = ref(null)

// 表单
const initForm = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

const editForm = reactive({
  isHoliday: 0,
  holidayName: '',
  weather: ''
})

// 星期标题
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// 星期映射（JS的getDay()返回0-6，0是周日）
const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

// 天气图标映射
const getWeatherIcon = (weather) => {
  if (!weather) return ''
  if (weather.includes('晴')) return '☀️'
  if (weather.includes('多云') || weather.includes('阴')) return '☁️'
  if (weather.includes('小雨') || weather.includes('雨')) return '🌧️'
  if (weather.includes('大雨') || weather.includes('雷')) return '⛈️'
  if (weather.includes('雪')) return '❄️'
  if (weather.includes('热')) return '🌡️'
  return '🌤️'
}

// 获取日历数据
const fetchCalendarData = async () => {
  loading.value = true
  try {
    const res = await request.get('/calendar/month', {
      params: { year: currentYear.value, month: currentMonth.value }
    })
    calendarData.value = res.data || []
  } catch (error) {
    console.error('获取日历数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 计算日历格子
const calendarCells = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value

  // 获取该月第一天和最后一天
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)

  // 该月第一天是星期几（转为周一=0）
  let firstDayOfWeek = firstDay.getDay() - 1
  if (firstDayOfWeek < 0) firstDayOfWeek = 6

  // 该月天数
  const daysInMonth = lastDay.getDate()

  // 今天
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  // 构建格子数组
  const cells = []

  // 上月补充格子
  const prevMonthLastDay = new Date(year, month - 1, 0).getDate()
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    cells.push({
      day: prevMonthLastDay - i,
      currentMonth: false,
      date: null
    })
  }

  // 当月格子
  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const data = calendarData.value.find(d => d.date === dateStr)

    cells.push({
      day,
      currentMonth: true,
      date: dateStr,
      isToday: dateStr === todayStr,
      // 前端兜底计算（如果后端没返回）
      dayOfWeekText: data?.dayOfWeekText || weekMap[new Date(dateStr).getDay()],
      weatherIcon: data?.weatherIcon || (data?.weather ? getWeatherIcon(data.weather) : ''),
      ...(data || {})
    })
  }

  // 下月补充格子（补满6行）
  const remainingCells = 42 - cells.length
  for (let i = 1; i <= remainingCells; i++) {
    cells.push({
      day: i,
      currentMonth: false,
      date: null
    })
  }

  return cells
})

// 月份切换
const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
  fetchCalendarData()
}

const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
  fetchCalendarData()
}

const goToday = () => {
  const today = new Date()
  currentYear.value = today.getFullYear()
  currentMonth.value = today.getMonth() + 1
  fetchCalendarData()
}

// 初始化
const handleInit = () => {
  initForm.year = currentYear.value
  initForm.month = currentMonth.value
  initDialogVisible.value = true
}

const submitInit = async () => {
  initLoading.value = true
  try {
    const res = await request.post('/calendar/init', initForm)
    ElMessage.success(res.data)
    initDialogVisible.value = false
    fetchCalendarData()
  } catch (error) {
    console.error('初始化失败:', error)
  } finally {
    initLoading.value = false
  }
}

// 编辑
const handleCellClick = (cell) => {
  if (!cell.id) {
    ElMessage.warning('请先初始化该月份的日历数据')
    return
  }

  currentCell.value = cell
  editForm.isHoliday = cell.isHoliday || 0
  editForm.holidayName = cell.holidayName || ''
  editForm.weather = cell.weather || ''
  editDrawerVisible.value = true
}

const submitEdit = async () => {
  editLoading.value = true
  try {
    await request.put(`/calendar/${currentCell.value.id}`, editForm)
    ElMessage.success('更新成功')
    editDrawerVisible.value = false
    fetchCalendarData()
  } catch (error) {
    console.error('更新失败:', error)
  } finally {
    editLoading.value = false
  }
}

// 初始化
onMounted(() => {
  fetchCalendarData()
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

.calendar-card {
  border: none;
  border-radius: 12px;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.month-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 20px;
  min-width: 120px;
  text-align: center;
}

/* 日历网格 */
.calendar-grid {
  padding: 16px 0;
}

.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 8px;
}

.week-cell {
  text-align: center;
  padding: 12px 0;
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

.date-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.date-cell {
  min-height: 80px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
  background-color: #fff;
}

.date-cell:hover {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.date-cell.other-month {
  background-color: #fafafa;
  cursor: default;
}

.date-cell.other-month:hover {
  background-color: #fafafa;
  border-color: transparent;
}

.date-cell.is-weekend {
  background-color: #f5f7fa;
}

.date-cell.is-holiday {
  background-color: #fef0f0;
}

.date-cell.is-today {
  border: 2px solid #409eff;
}

.date-number {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 4px;
}

.tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.weekend-tag {
  background-color: #e6f7ff;
  color: #1890ff;
}

.holiday-tag {
  background-color: #fff1f0;
  color: #f5222d;
}

.weather {
  display: flex;
  align-items: center;
  gap: 4px;
}

.weather-icon {
  font-size: 14px;
}

.weather-text {
  font-size: 12px;
  color: #909399;
}

/* 图例 */
.legend {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
  margin-top: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.weekend {
  background-color: #1890ff;
}

.dot.holiday {
  background-color: #f5222d;
}

.dot.weather-dot {
  background: linear-gradient(135deg, #ffd700, #ff6b6b);
}

/* 初始化弹窗 */
.init-tips {
  margin-top: 16px;
}

/* 编辑抽屉 */
.edit-form {
  padding: 0 20px;
}

.edit-date {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
</style>