<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps({
  collapsed: { type: Boolean, default: false },
})

const route = useRoute()
const router = useRouter()

const active = computed(() => route.path)

const menu = [
  {
    key: '/dashboard',
    title: '首页控制台',
    icon: 'Odometer',
    type: 'item',
  },
  {
    key: '/base',
    title: '商品档案管理',
    icon: 'Files',
    type: 'sub',
    children: [
      { key: '/base/category', title: '商品分类', icon: 'CollectionTag' },
      { key: '/base/product', title: '商品中心', icon: 'Goods' },
    ],
  },
  {
    key: '/sales',
    title: '销售数据中心',
    icon: 'DataAnalysis',
    type: 'sub',
    children: [
      { key: '/sales/import', title: '销售日结导入', icon: 'Upload' },
      { key: '/sales/list', title: '销售流水查询', icon: 'List' },
      { key: '/sales/analysis', title: '销售统计分析', icon: 'TrendCharts' },
      { key: '/sales/calendar', title: '日历因子管理', icon: 'Calendar' },
    ],
  },
  {
    key: '/forecasting',
    title: '智能销量预测',
    icon: 'TrendCharts',
    type: 'item',
  },
  {
    key: '/restocking',
    title: '智能补货建议',
    icon: 'Van',
    type: 'item',
  },
  {
    key: '/inventory',
    title: '库存预警与监控',
    icon: 'Box',
    type: 'sub',
    children: [
      { key: '/inventory/dashboard', title: '实时库存盘点', icon: 'PieChart' },
      { key: '/inventory/log', title: '库存变动流水', icon: 'List' },
    ],
  },
  {
    key: '/settings',
    title: '系统安全管理',
    icon: 'Setting',
    type: 'sub',
    children: [
      { key: '/settings/profile', title: '个人中心', icon: 'User' },
      { key: '/settings/log', title: '操作日志', icon: 'Document' },
    ],
  },
]

function go(path) {
  router.push(path)
}
</script>

<template>
  <aside class="sidebar">
    <div class="logo" @click="go('/dashboard')">
      <div class="logo-badge">
        <el-icon class="logo-cart" size="22">
          <ShoppingCart />
        </el-icon>
      </div>
      <div v-show="!collapsed" class="logo-text">SMEs-STORE</div>
    </div>

    <el-scrollbar class="menu-scroll">
      <el-menu
        class="menu"
        :default-active="active"
        :collapse="collapsed"
        :collapse-transition="false"
        router
      >
        <template v-for="m in menu" :key="m.key">
          <el-sub-menu v-if="m.type === 'sub'" :index="m.key">
            <template #title>
              <el-icon class="m-icon"><component :is="m.icon" /></el-icon>
              <span class="m-title">{{ m.title }}</span>
            </template>
            <el-menu-item
              v-for="c in m.children"
              :key="c.key"
              :index="c.key"
            >
              <el-icon class="m-icon"><component :is="c.icon" /></el-icon>
              <span class="m-title">{{ c.title }}</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-else :index="m.key">
            <el-icon class="m-icon"><component :is="m.icon" /></el-icon>
            <span class="m-title">{{ m.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<style scoped>
.sidebar {
  height: 100vh;
  background: #1e1e2d;
  color: rgba(255, 255, 255, 0.86);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  cursor: pointer;
  user-select: none;
}

.logo-badge {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #111;
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.95), #ffe28a);
  box-shadow: 0 10px 24px rgba(255, 193, 7, 0.18);
}

.logo-cart {
  color: #111;
}

.logo-text {
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 0.8px;
  color: rgba(255, 255, 255, 0.9);
}

.menu-scroll {
  flex: 1;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  margin: 4px 10px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.78);
  transition: all 0.16s ease;
}

.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.9);
}

.menu :deep(.el-menu-item.is-active) {
  color: #ffc107 !important;
  background: rgba(255, 193, 7, 0.1) !important;
  box-shadow: 0 10px 26px rgba(255, 193, 7, 0.12);
}

.menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #ffc107 !important;
}

.menu :deep(.el-sub-menu__title .el-sub-menu__icon-arrow) {
  color: rgba(255, 255, 255, 0.55);
}

.m-icon {
  margin-right: 10px;
}

.menu :deep(.el-menu-item .el-icon),
.menu :deep(.el-sub-menu__title .el-icon) {
  color: inherit;
}

.menu :deep(.el-menu--inline) {
  background: transparent;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  padding-left: 14px !important;
}
</style>
