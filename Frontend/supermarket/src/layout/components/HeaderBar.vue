<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  collapsed: { type: Boolean, default: false },
})

const emit = defineEmits(['toggle'])

const route = useRoute()
const router = useRouter()

const user = ref({})
try {
  user.value = JSON.parse(localStorage.getItem('user') || '{}')
} catch {
  user.value = {}
}

const displayName = computed(() => user.value?.realName || user.value?.nickname || user.value?.name || '管理员')
const avatarText = computed(() => (displayName.value || 'A').slice(0, 1).toUpperCase())

const crumbs = computed(() =>
  (route.matched || [])
    .filter((r) => r.meta && r.meta.title)
    .map((r) => ({
      path: r.path,
      title: r.meta.title,
    })),
)

const beijingTimeText = ref('')
let timer = null

function updateBeijingTimeText() {
  const now = new Date()
  const parts = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now)

  const map = {}
  for (const p of parts) {
    if (p.type !== 'literal') map[p.type] = p.value
  }

  const ymd = `${map.year}年${map.month}月${map.day}日`
  const weekday = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    weekday: 'long',
  }).format(now)

  beijingTimeText.value = `${ymd} ${weekday}`
}

onMounted(() => {
  updateBeijingTimeText()
  timer = setInterval(updateBeijingTimeText, 30 * 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  timer = null
})

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
</script>

<template>
  <header class="header">
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

    <div class="right">
      <el-tooltip content="通知" placement="bottom">
        <el-button class="icon-btn" text>
          <el-icon size="18"><Bell /></el-icon>
        </el-button>
      </el-tooltip>

      <div class="bj-time">{{ beijingTimeText }}</div>

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
