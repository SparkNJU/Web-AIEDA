<script setup lang="ts">
import { ElInput, ElButton } from 'element-plus'
import { ArrowUp } from '@element-plus/icons-vue'

// 接收参数
const props = defineProps<{
  inputMessage: string
  isLoading: boolean
  inputDisabled: boolean
  isStreamMode?: boolean
  isStreaming?: boolean
}>()

// 事件传递
const emit = defineEmits<{
  'update:input-message': [value: string]
  'send-message': [message: string]
}>()

// 发送消息（按回车）
const handleKeyup = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 发送消息（点击按钮）
const sendMessage = () => {
  if (props.inputMessage.trim() && !props.inputDisabled) {
    emit('send-message', props.inputMessage.trim())
  }
}
</script>

<template>
  <div class="chat-input">
    <!-- 输入框和发送按钮行 -->
    <div class="input-row">
      <el-input 
        :model-value="inputMessage"
        type="textarea" 
        :rows="1" 
        :autosize="{ minRows: 1, maxRows: 4 }"
        resize="none" 
        placeholder="输入您的问题..."
        @keyup="handleKeyup"
        :disabled="inputDisabled"
        @update:model-value="(val: string) => emit('update:input-message', val)"
        class="message-input"
      />
      <!-- 发送按钮 -->
      <el-button 
        type="primary" 
        @click="sendMessage" 
        :loading="isLoading || isStreaming"
        :disabled="inputDisabled || !inputMessage.trim()"
        :icon="ArrowUp"
        class="send-button"
        style="background-color: rgb(102, 8, 116); border-color: rgb(102, 8, 116);"
        title="发送消息"
        round
      >
        {{ isStreaming ? '生成中...' : '发送' }}
      </el-button>
    </div>

    <!-- 底部提示 -->
    <div class="input-footer">
      <div class="input-tips">
        按 Enter 发送，Shift + Enter 换行
        <span v-if="isStreamMode" class="stream-hint">
          · 流式模式：AI将实时回复
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #e0e0e0;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0; /* 防止被压缩 */
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.message-input {
  flex: 1;
}

.send-button {
  flex-shrink: 0;
  height: 40px;
  min-width: 80px;
}

.input-footer {
  display: flex;
  justify-content: center;
}

.input-tips {
  font-size: 0.75em;
  color: #999;
  text-align: center;
}

.stream-hint {
  color: rgb(102, 8, 116);
  font-weight: 500;
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
  border-color: #dcdfe6;
  padding: 12px;
  line-height: 1.4;
}

:deep(.el-textarea__inner):focus {
  border-color: rgb(102, 8, 116);
}
</style>
