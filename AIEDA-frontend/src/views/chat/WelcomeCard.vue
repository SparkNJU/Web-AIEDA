<script setup lang="ts">
import { ElCard, ElMenu, ElMenuItem } from 'element-plus'

// 接收参数
const props = defineProps<{
  suggestions: string[]
  hasFiles?: boolean // 新增：是否有文件上传
}>()

// 事件传递
const emit = defineEmits<{
  'insert-question': [question: string]
}>()

// 使用props防止TypeScript警告
const { suggestions, hasFiles } = props
</script>

<template>
  <div class="welcome-container chat-theme">
    <el-card class="welcome-card welcome-card-theme chat-card" shadow="never">
      <div class="welcome-content">
        <h2>🤖 智能助手</h2>
        <p>欢迎使用Orvix平台的智能助手！我可以帮助您解答各种问题。</p>
        
        <div class="suggestions">
          <h3>💡 推荐问题</h3>
          <el-menu 
            class="suggestion-menu" 
            mode="vertical"
            :default-active="''"
            @select="(index: string) => emit('insert-question', suggestions[parseInt(index)])"
          >
            <el-menu-item 
              v-for="(suggestion, index) in (hasFiles ? suggestions.slice(0, 2) : suggestions)" 
              :key="index"
              :index="index.toString()"
              class="suggestion-item suggestion-item-theme"
            >
              {{ suggestion }}
            </el-menu-item>
          </el-menu>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.welcome-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: var(--chat-bg-primary);
  overflow-y: auto;
  min-height: 0;
  height: 100%;
}

.welcome-card {
  max-width: 700px;
  width: 100%;
  height: fit-content;
  max-height: 90%;
  border-radius: 12px;
  margin: auto;
  display: flex;
  flex-direction: column;
}

.welcome-content {
  text-align: center;
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.welcome-content h2 {
  color: var(--chat-text-primary) !important;
  font-size: 1.8rem;
  margin-bottom: 0.8rem;
  font-weight: 500;
}

.welcome-content p {
  color: var(--chat-text-secondary) !important;
  font-size: 1rem;
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.suggestions {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.suggestions h3 {
  color: var(--chat-text-primary) !important;
  margin-bottom: 1rem;
  font-size: 1.1rem;
  font-weight: 500;
}

.suggestion-menu {
  background: transparent;
  border: none;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}

.suggestion-item {
  height: 60px !important;
  line-height: 60px;
  text-align: center;
  margin-bottom: 0.8rem;
  border-radius: 8px;
  background-color: transparent;
  color: var(--chat-text-secondary) !important;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  white-space: normal;
  padding: 14px 18px;
}

.suggestion-item:last-child {
  margin-bottom: 0;
}

/* 移除Element Plus默认样式 */
.suggestion-menu .el-menu-item.is-active {
  background-color: var(--chat-primary-light) !important;
  color: var(--chat-text-primary) !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .suggestion-menu {
    max-width: 100%;
  }
  
  .welcome-container {
    padding: 0.5rem;
  }
  
  .welcome-content {
    padding: 16px;
  }
  
  .suggestion-item {
    height: 50px !important;
    line-height: 50px;
    font-size: 0.85rem;
  }
}
</style>
