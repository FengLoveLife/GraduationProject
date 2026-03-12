<script setup></script>

<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </transition>
    </router-view>
  </section>
</template>

<style scoped>
.app-main {
  padding: 16px;
  background: #f3f4f7;
  min-height: calc(100vh - 64px);
}

:global(.fade-transform-enter-active),
:global(.fade-transform-leave-active) {
  transition: all 0.2s ease;
}

:global(.fade-transform-enter-from) {
  opacity: 0;
  transform: translateY(10px);
}

:global(.fade-transform-leave-to) {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
