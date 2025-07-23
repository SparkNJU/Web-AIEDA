<!-- MessageList.vue -->
<script setup lang="ts">
import { ElScrollbar, ElRow, ElCol } from 'element-plus'
import MessageBubble from './MessageBubble.vue'
import type { ChatRecord } from './ChatPage.vue'

// 接收消息列表
const props = defineProps<{
  messages: ChatRecord[]
}>()

// 使用props防止TypeScript警告
const { messages } = props
</script>

<template>
  <el-scrollbar class="chat-messages" ref="messagesContainer">
    <el-row 
      v-for="(msg, index) in messages" 
      :key="index" 
      :justify="msg.direction ? 'end' : 'start'"
      class="message-row"
    >
      <el-col>
        <MessageBubble 
          :content="msg.content"
          :is-user="msg.direction"
        />
      </el-col>
    </el-row>
  </el-scrollbar>
</template>

<style scoped>
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background-color: #fafafa;
  min-height: 0; /* 允许收缩 */
}

.message-row { 
  margin: 12px 0; 
}

.message-row:first-child {
  margin-top: 0;
}

.message-row:last-child {
  margin-bottom: 0;
}
</style>