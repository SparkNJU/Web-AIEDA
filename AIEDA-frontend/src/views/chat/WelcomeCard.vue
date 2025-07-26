<script setup lang="ts">
import { ElCard, ElButton } from 'element-plus'

// 接收参数
const props = defineProps<{
  suggestions: string[]
}>()

// 事件传递
const emit = defineEmits<{
  'insert-question': [question: string]
}>()

// 使用props防止TypeScript警告
const { suggestions } = props
</script>

<template>
  <div class="welcome-container">
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-content">
        <h2>🤖 智能助手</h2>
        <p>欢迎使用AIEDA平台的智能助手！我可以帮助您解答各种问题。</p>
        
        <div class="suggestions">
          <h3>💡 推荐问题</h3>
          <div class="suggestion-grid">
            <el-button 
              v-for="(suggestion, index) in suggestions" 
              :key="index"
              class="suggestion-btn"
              plain
              @click="emit('insert-question', suggestion)"
            >
              {{ suggestion }}
            </el-button>
          </div>
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

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); /* 使用grid布局，自适应列数 */
  gap: 0.8rem;
  width: 100%;
  margin: 0 auto;
}

.suggestion-btn {
  text-align: left;
  white-space: normal;
  height: auto;
  padding: 14px 18px; /* 稍微减少内边距 */
  border-color: rgba(102, 8, 116, 0.3);
  color: rgb(102, 8, 116);
  border-radius: 8px;
  transition: all 0.2s ease;
  width: 100%; /* 确保所有按钮宽度一致 */
  min-height: 48px; /* 稍微减少最小高度 */
  display: flex;
  align-items: center;
  justify-content: flex-start;
  font-size: 0.9rem; /* 稍微减小字体 */
  line-height: 1.4;
}

.suggestion-btn:hover {
  background-color: rgba(102, 8, 116, 0.08);
  border-color: rgb(102, 8, 116);
  color: rgb(102, 8, 116);
  transform: translateY(-1px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .suggestion-grid {
    grid-template-columns: 1fr; /* 小屏幕时单列显示 */
  }
  
  .welcome-container {
    padding: 0.5rem;
  }
  
  .welcome-content {
    padding: 16px;
  }
}
</style>
