<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const title = computed(() => route.meta?.title || '模块')
</script>

<template>
  <div class="ph-wrap">
    <div class="ph-card">
      <div class="ph-icon">
        <el-icon size="32">
          <Box />
        </el-icon>
      </div>
      <div class="ph-title">{{ title }}</div>
      <div class="ph-sub">模块正在开发中...</div>
    </div>
  </div>
</template>

<style scoped>
.ph-wrap {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.ph-card {
  width: min(640px, 92vw);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 18px;
  padding: 36px 28px;
  box-shadow: 0 18px 50px rgba(17, 24, 39, 0.08);
  border: 1px solid rgba(15, 23, 42, 0.06);
  text-align: center;
  backdrop-filter: blur(8px);
}

.ph-icon {
  width: 68px;
  height: 68px;
  margin: 0 auto 14px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: #111;
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.9), #ffd966);
  box-shadow: 0 10px 24px rgba(255, 193, 7, 0.22);
}

.ph-title {
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: 0.4px;
}

.ph-sub {
  margin-top: 10px;
  font-size: 13px;
  color: #64748b;
}
</style>
