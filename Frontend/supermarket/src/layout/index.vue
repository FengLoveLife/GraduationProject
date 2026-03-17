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

    <div class="main" :class="{ collapsed }">
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
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  width: 240px;
  transition: width 0.18s ease;
  flex: none;
  z-index: 100;
}
.sider.collapsed {
  width: 64px;
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  margin-left: 240px;
  transition: margin-left 0.18s ease;
}

.main.collapsed {
  margin-left: 64px;
}
</style>
