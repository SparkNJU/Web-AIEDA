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
  padding: 2rem;
  background-color: #fafafa;
  overflow-y: auto;
  min-height: 0; /* 允许收缩 */
}

.welcome-card {
  max-width: 560px;
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin: auto; /* 确保居中 */
}

.welcome-content {
  text-align: center;
  padding: 24px;
}

.welcome-content h2 {
  color: rgb(102, 8, 116);
  font-size: 1.8rem;
  margin-bottom: 1rem;
  font-weight: 500;
}

.welcome-content p {
  color: #666;
  font-size: 1rem;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.suggestions h3 {
  color: rgb(102, 8, 116);
  margin-bottom: 1.2rem;
  font-size: 1.1rem;
  font-weight: 500;
}

.suggestion-grid {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  max-width: 480px; /* 限制按钮最大宽度 */
  margin: 0 auto; /* 居中显示 */
}

.suggestion-btn {
  text-align: left;
  white-space: normal;
  height: auto;
  padding: 16px 20px;
  border-color: rgba(102, 8, 116, 0.3);
  color: rgb(102, 8, 116);
  border-radius: 8px;
  transition: all 0.2s ease;
  width: 100%; /* 确保所有按钮宽度一致 */
  min-height: 50px; /* 设置最小高度保证一致性 */
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.suggestion-btn:hover {
  background-color: rgba(102, 8, 116, 0.08);
  border-color: rgb(102, 8, 116);
  color: rgb(102, 8, 116);
  transform: translateY(-1px);
}
</style>
