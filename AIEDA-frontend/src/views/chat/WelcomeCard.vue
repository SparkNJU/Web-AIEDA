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
  <div class="welcome-container">
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-content">
        <h2>🤖 智能助手</h2>
        <p>欢迎使用CPPO平台的智能助手！我可以帮助您解答各种问题。</p>
        
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
              class="suggestion-item"
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
  padding: 1rem; /* 减少padding以减少留白 */
  background-color: #fafafa;
  overflow-y: auto;
  min-height: 0; /* 允许收缩 */
  height: 100%; /* 确保填满父容器 */
}

.welcome-card {
  max-width: 700px; /* 增加最大宽度 */
  width: 100%;
  height: fit-content;
  max-height: 90%; /* 限制最大高度避免溢出 */
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin: auto; /* 确保居中 */
  display: flex;
  flex-direction: column;
}

.welcome-content {
  text-align: center;
  padding: 20px; /* 减少内边距 */
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.welcome-content h2 {
  color: rgb(102, 8, 116);
  font-size: 1.8rem;
  margin-bottom: 0.8rem; /* 减少间距 */
  font-weight: 500;
}

.welcome-content p {
  color: #666;
  font-size: 1rem;
  margin-bottom: 1.5rem; /* 减少间距 */
  line-height: 1.6;
}

.suggestions {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.suggestions h3 {
  color: rgb(102, 8, 116);
  margin-bottom: 1rem; /* 减少间距 */
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
  height: 60px !important; /* 固定高度确保所有项目一致 */
  line-height: 60px;
  text-align: center;
  margin-bottom: 0.8rem;
  border: 1px solid rgba(102, 8, 116, 0.3);
  border-radius: 8px;
  color: rgb(102, 8, 116) !important;
  background-color: transparent;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  white-space: normal;
  padding: 14px 18px;
}

.suggestion-item:hover {
  background-color: rgba(102, 8, 116, 0.08) !important;
  border-color: rgb(102, 8, 116);
  color: rgb(102, 8, 116) !important;
  transform: translateY(-1px);
}

.suggestion-item:last-child {
  margin-bottom: 0;
}

/* 移除Element Plus默认样式 */
.suggestion-menu .el-menu-item.is-active {
  background-color: rgba(102, 8, 116, 0.08) !important;
  color: rgb(102, 8, 116) !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .suggestion-menu {
    max-width: 100%; /* 小屏幕时使用全宽 */
  }
  
  .welcome-container {
    padding: 0.5rem;
  }
  
  .welcome-content {
    padding: 16px;
  }
  
  .suggestion-item {
    height: 50px !important; /* 小屏幕时稍微减少高度 */
    line-height: 50px;
    font-size: 0.85rem;
  }
}
</style>
