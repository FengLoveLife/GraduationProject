<script setup></script>

<template>
  <div class="wrap">
    <div class="card">
      <div class="title">Dashboard</div>
      <div class="desc">这里是首页占位页，后续替换为你的业务看板。</div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: #fafafa;
}
.card {
  width: 520px;
  max-width: 92vw;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  text-align: center;
}
.title {
  font-size: 22px;
  font-weight: 800;
  color: #111;
}
.desc {
  margin-top: 10px;
  font-size: 14px;
  color: #666;
}
</style>

