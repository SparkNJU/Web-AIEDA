<!-- MessageList.vue -->
<script setup lang="ts">
import { ElScrollbar, ElRow, ElCol } from 'element-plus'
import MessageBubble from './MessageBubble.vue'
import type { ChatRecord } from './ChatPage.vue'

// 接收消息列表
const props = defineProps<{
  messages: ChatRecord[]
}>()

// 不要解构props，直接使用props.messages来保持响应式
</script>

<template>
  <el-scrollbar class="chat-messages" ref="messagesContainer">
    <el-row 
      v-for="(msg, index) in props.messages" 
      :key="`row-${msg.rid || msg.sid || 'temp'}-${index}`"
      :justify="msg.direction ? 'end' : 'start'"
      class="message-row"
    >
      <el-col>
        <MessageBubble 
          :key="`msg-${msg.rid || msg.sid || 'temp'}-${index}-${msg.content?.length || 0}`"
          :content="msg.content"
          :is-user="msg.direction"
          :is-streaming="msg.isStreaming"
          :is-error="msg.isError"
        />
      </el-col>
    </el-row>
  </el-scrollbar>
</template>

<style scoped>
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px; /* 减少padding */
  background-color: #fafafa;
  min-height: 0; /* 允许收缩 */
  height: 100%; /* 确保填满父容器 */
  box-sizing: border-box;
}

.message-row { 
  margin: 8px 0; /* 减少消息间距 */
}

.message-row:first-child {
  margin-top: 0;
}

.message-row:last-child {
  margin-bottom: 0;
}

/* 确保滚动条样式 */
.chat-messages :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.chat-messages :deep(.el-scrollbar__view) {
  height: 100%;
  min-height: fit-content;
}
</style>