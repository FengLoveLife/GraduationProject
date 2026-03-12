<script setup>
import { ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import HeaderBar from './components/HeaderBar.vue'
import AppMain from './components/AppMain.vue'

const collapsed = ref(false)
</script>

<template>
  <div class="layout">
    <div class="sider" :class="{ collapsed }">
      <Sidebar :collapsed="collapsed" />
    </div>

    <div class="main">
      <HeaderBar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
      <AppMain />
    </div>
  </div>
</template>

<style scoped>
.layout {
  width: 100%;
  min-height: 100vh;
  display: flex;
  background: #f3f4f7;
}

.sider {
  width: 240px;
  transition: width 0.18s ease;
  flex: none;
}
.sider.collapsed {
  width: 64px;
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
</style>
