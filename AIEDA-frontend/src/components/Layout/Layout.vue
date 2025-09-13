<template>
  <div class="app-layout">
    <Header />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import Header from './Header.vue'
</script>

<style>
/* 全局样式 - 影响整个应用 */
html, body {
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden; /* 防止页面级别的滚动 */
}

#app {
  height: 100vh;
  overflow: hidden;
}
</style>

<style scoped>
.app-layout {
  height: 100vh; /* 固定为视口高度，不允许超出 */
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: hidden; /* 防止内容超出视口高度 */
}

.main-content {
  flex: 1;
  background: var(--bg-primary);
  height: 100vh; /* 使用全视口高度 */
  overflow: auto; /* 内容区域如果需要可以滚动 */
  box-sizing: border-box; /* 确保padding和border包含在高度计算中 */
}

/* 移动端适配 */
@media (max-width: 768px) {
  .main-content {
    padding: 0; /* 移动端移除所有内边距，让各页面自己处理Header空间 */
  }
}

/* 夜间模式样式 */
[data-theme="dark"] .app-layout {
  background: #0f0f0f;
}

[data-theme="dark"] .main-content {
  background: #0f0f0f;
}
</style>