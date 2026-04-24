<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUnreadAlerts, markAllRead, getRecentAlerts } from '@/api/alert'

const props = defineProps({
  collapsed: { type: Boolean, default: false },
})
const emit = defineEmits(['toggle'])

const route  = useRoute()
const router = useRouter()

const user = ref({})
try {
  user.value = JSON.parse(localStorage.getItem('user') || '{}')
} catch {
  user.value = {}
}

const displayName = computed(() => user.value?.realName || user.value?.nickname || user.value?.name || '管理员')
const avatarText  = computed(() => (displayName.value || 'A').slice(0, 1).toUpperCase())

const crumbs = computed(() =>
  (route.matched || [])
    .filter((r) => r.meta && r.meta.title)
    .map((r) => ({ path: r.path, title: r.meta.title })),
)

// ==================== 时间 ====================

const beijingTimeText = ref('')
let clockTimer = null

function updateBeijingTimeText() {
  const now   = new Date()
  const parts = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(now)

  const map = {}
  for (const p of parts) {
    if (p.type !== 'literal') map[p.type] = p.value
  }
  const ymd     = `${map.year}年${map.month}月${map.day}日`
  const weekday = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai', weekday: 'long',
  }).format(now)
  beijingTimeText.value = `${ymd} ${weekday}`
}

// ==================== 通知 ====================

const notifyVisible  = ref(false)
const unreadCount    = ref(0)
const alertList      = ref([])
const showHistory    = ref(false)
const historyList    = ref([])
const currentList    = computed(() => showHistory.value ? historyList.value : alertList.value)
let   pollTimer      = null

const alertTypeMap = {
  1: { label: '库存告急', tagType: 'danger'  },
  2: { label: '节日预警', tagType: 'warning' },
}

async function fetchUnread() {
  try {
    const res = await getUnreadAlerts()
    if (res.code === 200 && res.data) {
      unreadCount.value = res.data.count || 0
      alertList.value   = res.data.list  || []
    }
  } catch {
    // 静默失败，不影响页面
  }
}

async function onNotifyOpen() {
  showHistory.value = false
  historyList.value = []
  if (unreadCount.value > 0) {
    try {
      await markAllRead()
      unreadCount.value = 0
      alertList.value = alertList.value.map(item => ({ ...item, isRead: 1 }))
    } catch { /* ignore */ }
  }
}

async function onViewHistory() {
  showHistory.value = true
  try {
    const res = await getRecentAlerts()
    if (res.code === 200 && Array.isArray(res.data)) {
      historyList.value = res.data
    }
  } catch { /* ignore */ }
}

function formatTime(createTime) {
  if (!createTime) return ''
  const d = new Date(createTime)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

// ==================== 登出 ====================

async function logout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    closeOnClickModal: false,
  })
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  await router.replace('/login')
}

// ==================== 生命周期 ====================

onMounted(() => {
  updateBeijingTimeText()
  clockTimer = setInterval(updateBeijingTimeText, 30 * 1000)

  fetchUnread()
  pollTimer = setInterval(fetchUnread, 5 * 60 * 1000) // 每5分钟轮询一次
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (pollTimer)  clearInterval(pollTimer)
})
</script>

<template>
  <header class="header">
    <!-- 左侧：折叠按钮 + 面包屑 -->
    <div class="left">
      <el-button class="collapse-btn" text @click="emit('toggle')">
        <el-icon size="18">
          <component :is="props.collapsed ? 'Expand' : 'Fold'" />
        </el-icon>
      </el-button>

      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item v-for="c in crumbs" :key="c.path">
          {{ c.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右侧：通知 + 时间 + 用户 -->
    <div class="right">

      <!-- 通知铃铛 -->
      <el-popover
        v-model:visible="notifyVisible"
        placement="bottom-end"
        :width="380"
        trigger="click"
        popper-class="notify-popper"
        @show="onNotifyOpen"
      >
        <template #reference>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notify-badge">
            <el-button class="icon-btn" text>
              <el-icon size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
        </template>

        <!-- 通知面板 -->
        <div class="notify-panel">
          <div class="notify-header">
            <el-button v-if="showHistory" class="back-btn" text size="small" @click="showHistory = false">
              <el-icon><ArrowLeft /></el-icon>返回
            </el-button>
            <span class="notify-title">{{ showHistory ? '历史消息（近7天）' : '系统预警通知' }}</span>
            <template v-if="!showHistory">
              <span class="notify-sub" v-if="alertList.length > 0">共 {{ alertList.length }} 条</span>
              <span class="notify-sub" v-else>暂无未读通知</span>
              <el-button class="history-link" text size="small" @click="onViewHistory">查看历史</el-button>
            </template>
          </div>

          <div class="notify-list" v-if="currentList.length > 0">
            <div
              v-for="item in currentList"
              :key="item.id"
              class="notify-item"
            >
              <div class="notify-item-top">
                <el-tag
                  :type="alertTypeMap[item.alertType]?.tagType || 'info'"
                  size="small"
                  effect="dark"
                  class="notify-tag"
                >
                  {{ alertTypeMap[item.alertType]?.label || '预警' }}
                </el-tag>
                <span class="notify-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="notify-content">{{ item.alertContent }}</div>
            </div>
            <div v-if="showHistory" class="notify-history-hint">只展示最近 7 天的通知</div>
          </div>

          <div class="notify-empty" v-else>
            <el-icon size="40" color="#dcdfe6"><Bell /></el-icon>
            <p>{{ showHistory ? '近7天暂无预警通知' : '当前没有预警通知' }}</p>
          </div>
        </div>
      </el-popover>

      <div class="bj-time">{{ beijingTimeText }}</div>

      <!-- 用户下拉 -->
      <el-dropdown trigger="click">
        <div class="user">
          <div class="avatar">{{ avatarText }}</div>
          <div class="name">{{ displayName }}</div>
          <el-icon class="caret" size="14"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.06);
  position: sticky;
  top: 0;
  z-index: 20;
}

.left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.collapse-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  color: #111;
  transition: all 0.16s ease;
}
.collapse-btn:hover {
  background: rgba(255, 193, 7, 0.14);
  color: #7c5b00;
}

.breadcrumb {
  min-width: 0;
}
.breadcrumb :deep(.el-breadcrumb__inner) {
  color: #334155;
  font-weight: 700;
}
.breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: #7c5b00;
}
.breadcrumb :deep(.el-breadcrumb__separator) {
  color: #cbd5e1;
}

.right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 铃铛角标 */
.notify-badge :deep(.el-badge__content) {
  font-size: 10px;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  transition: all 0.16s ease;
}
.icon-btn:hover {
  background: rgba(15, 23, 42, 0.06);
}

.bj-time {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  padding: 7px 10px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.06);
  user-select: none;
  white-space: nowrap;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 6px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.16s ease;
  user-select: none;
}
.user:hover {
  background: rgba(15, 23, 42, 0.05);
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 900;
  color: #111;
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.95), #ffe28a);
  box-shadow: 0 10px 20px rgba(255, 193, 7, 0.2);
}

.name {
  max-width: 180px;
  font-size: 13px;
  font-weight: 800;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  color: #94a3b8;
}
</style>

<!-- 通知面板样式（非 scoped，作用于 popper） -->
<style>
.notify-popper {
  padding: 0 !important;
  border-radius: 16px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12) !important;
  overflow: hidden;
}

.notify-panel {
  display: flex;
  flex-direction: column;
  max-height: 440px;
}

.notify-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 18px 12px;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;
}
.notify-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  flex: 1;
}
.notify-sub {
  font-size: 12px;
  color: #94a3b8;
}

.notify-list {
  overflow-y: auto;
  flex: 1;
  padding: 8px 0;
  scrollbar-width: thin;
  scrollbar-color: #e2e8f0 transparent;
}
.notify-list::-webkit-scrollbar       { width: 4px; }
.notify-list::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 2px; }

.notify-item {
  padding: 12px 18px;
  border-bottom: 1px solid #f8fafc;
  transition: background 0.15s;
  cursor: default;
}
.notify-item:last-child { border-bottom: none; }
.notify-item:hover      { background: #f8fafc; }

.notify-item-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.notify-tag {
  flex-shrink: 0;
}
.notify-time {
  font-size: 11px;
  color: #94a3b8;
  margin-left: auto;
}

.notify-content {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
}

.notify-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  color: #94a3b8;
  font-size: 13px;
  gap: 10px;
}

.back-btn {
  font-size: 12px !important;
  color: #64748b !important;
  padding: 0 4px !important;
  margin-right: 2px;
}

.history-link {
  font-size: 12px !important;
  color: #94a3b8 !important;
  padding: 0 4px !important;
}

.notify-history-hint {
  text-align: center;
  padding: 14px 0 18px;
  font-size: 11px;
  color: #cbd5e1;
  letter-spacing: 0.02em;
}
</style>
