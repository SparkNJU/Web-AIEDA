<script setup lang="ts">
import { onMounted } from 'vue'

// 初始化主题
onMounted(() => {
  const savedTheme = localStorage.getItem('theme') || 'dark'
  document.documentElement.setAttribute('data-theme', savedTheme)
})
</script>

<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 全局样式已在 theme.css 中定义 */
</style>