import { h } from 'vue'
import { createRouter, createWebHistory, RouterView } from 'vue-router'

const Login = () => import('../views/Login.vue')
const Layout = () => import('../layout/index.vue')
const Placeholder = () => import('../views/placeholder/index.vue')
const Category = () => import('../views/base/category/index.vue')
const Product = () => import('../views/base/product/index.vue')

const RouteView = { name: 'RouteView', render: () => h(RouterView) }

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: Login, meta: { title: '登录' } },
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: Placeholder,
          meta: { title: '首页控制台' },
        },
        {
          path: 'base',
          component: RouteView,
          redirect: '/base/category',
          meta: { title: '商品档案管理' },
          children: [
            {
              path: 'category',
              name: 'Category',
              component: Category,
              meta: { title: '商品分类' },
            },
            {
              path: 'product',
              name: 'Product',
              component: Product,
              meta: { title: '商品中心' },
            },
          ],
        },
        {
          path: 'sales',
          component: RouteView,
          redirect: '/sales/import',
          meta: { title: '销售数据中心' },
          children: [
            {
              path: 'import',
              name: 'SalesImport',
              component: () => import('../views/sales/import/index.vue'),
              meta: { title: '销售日结导入' },
            },
            {
              path: 'list',
              name: 'SalesList',
              component: () => import('../views/sales/list/index.vue'),
              meta: { title: '销售流水查询' },
            },
            {
              path: 'analysis',
              name: 'SalesAnalysis',
              component: () => import('../views/sales/analysis/index.vue'),
              meta: { title: '销售统计分析' },
            },
            {
              path: 'calendar',
              name: 'SalesCalendar',
              component: () => import('../views/sales/calendar/index.vue'),
              meta: { title: '日历因子管理' },
            },
          ],
        },
        {
          path: 'forecasting',
          name: 'Forecasting',
          component: Placeholder,
          meta: { title: '智能销量预测' },
        },
        {
          path: 'restocking',
          name: 'Restocking',
          component: Placeholder,
          meta: { title: '智能补货建议' },
        },
        {
          path: 'inventory',
          component: RouteView,
          redirect: '/inventory/dashboard',
          meta: { title: '库存预警与监控' },
          children: [
            {
              path: 'dashboard',
              name: 'InventoryDashboard',
              component: () => import('../views/inventory/dashboard/index.vue'),
              meta: { title: '实时库存盘点' },
            },
            {
              path: 'log',
              name: 'InventoryLog',
              component: () => import('../views/inventory/log/index.vue'),
              meta: { title: '库存变动流水' },
            },
          ],
        },
        {
          path: 'settings',
          component: RouteView,
          redirect: '/settings/profile',
          meta: { title: '系统安全管理' },
          children: [
            {
              path: 'profile',
              name: 'Profile',
              component: () => import('../views/settings/profile/index.vue'),
              meta: { title: '个人中心' },
            },
            {
              path: 'log',
              name: 'OperationLog',
              component: () => import('../views/settings/log/index.vue'),
              meta: { title: '操作日志' },
            },
          ],
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

/* 每次跳转前检查 localStorage 里有没有 token。 */
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    if (token) return '/'
    return true
  }

  if (!token) return '/login'
  return true
})

export default router

