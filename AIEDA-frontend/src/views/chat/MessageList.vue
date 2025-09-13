<!-- MessageList.vue -->
<script setup lang="ts">
import { ElScrollbar, ElRow, ElCol } from 'element-plus'
import MessageBubble from './MessageBubble.vue'
import type { ChatRecord } from './ChatPage.vue'
import type { FileVO } from '../../api/file'

// 接收消息列表
const props = defineProps<{
  messages: ChatRecord[]
  hasFilePreview?: boolean // 是否有文件预览面板展开
  uid?: number // 用户ID
  sid?: number // 会话ID
}>()

// 定义事件
const emit = defineEmits<{
  'open-file-preview': [file: FileVO] // 文件预览事件
  'send-confirmation': [choice: '1' | '2'] // 用户确认事件
}>()

// 处理文件预览事件
const handleFilePreview = (file: FileVO) => {
  console.log('MessageList: 转发文件预览事件', file.originalName)
  emit('open-file-preview', file)
}

// 处理用户确认事件
const handleUserConfirmation = (choice: '1' | '2') => {
  console.log('MessageList: 转发用户确认事件', choice)
  emit('send-confirmation', choice)
}

// 不要解构props，直接使用props.messages来保持响应式
</script>

<template>
  <el-scrollbar class="chat-messages" :class="{ 'with-file-preview': props.hasFilePreview }" ref="messagesContainer">
    <el-row 
      v-for="(msg, index) in props.messages" 
      :key="`row-${msg.rid || msg.sid || 'temp'}-${index}`"
      class="message-row"
      :justify="msg.direction ? 'end' : 'start'"
    >
      <el-col 
        :span="msg.direction ? 15 : 24"
        class="message-col"
      >
        <MessageBubble 
          :key="`msg-${msg.rid || msg.sid || 'temp'}-${index}-${msg.content?.length || 0}-${msg._updateTimestamp || msg._completeTimestamp || 0}`"
          :content="msg.content"
          :is-user="msg.direction"
          :is-streaming="msg.isStreaming"
          :is-error="msg.isError"
          :record-id="msg.rid"
          :attached-files="msg.attachedFiles"
          :uid="props.uid"
          :sid="props.sid"
          @open-file-preview="handleFilePreview"
          @send-confirmation="handleUserConfirmation"
        />
      </el-col>
    </el-row>
  </el-scrollbar>
</template>

<style scoped>
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  background-color: var(--chat-bg-primary);
  min-height: 0; /* 允许收缩 */
  height: 100%; /* 确保填满父容器 */
  width: 100%; /* 确保占满宽度 */
  box-sizing: border-box;
}

/* 夜间模式下的消息列表背景 */
[data-theme="dark"] .chat-messages {
  background-color: #0f0f0f;
}

.message-row { 
  margin: 8px 0;
  max-width: 100%; /* 确保行不会超出容器 */
  box-sizing: border-box;
}

.message-row:first-child {
  margin-top: 0;
}

.message-row:last-child {
  margin-bottom: 0;
}

/* 消息列样式 */
.message-col {
  max-width: 100%;
  box-sizing: border-box;
}

/* 用户消息：60%宽度，通过justify-end自然右对齐 */
.message-row[justify="end"] .message-col {
  width: 60%; /* 用户消息宽度 */
}

/* AI消息：100%宽度，左对齐 */
.message-row[justify="start"] .message-col {
  width: 100%; /* AI消息占满容器宽度 */
}

/* 当有文件预览面板时，调整消息宽度 */
.chat-messages.with-file-preview .message-row[justify="end"] .message-col {
  width: 80%; /* 文件预览时用户消息占80% */
}

.chat-messages.with-file-preview .message-row[justify="start"] .message-col {
  width: 100%; /* AI消息始终占满可用宽度 */
}

/* 确保列不会超出容器 */
.message-row .el-col {
  max-width: 100%;
  box-sizing: border-box;
}

/* 确保滚动条样式 */
.chat-messages :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
  width: 100%;
  box-sizing: border-box;
}

.chat-messages :deep(.el-scrollbar__view) {
  height: 100%;
  width: 100%;
  min-height: fit-content;
  box-sizing: border-box;
}

/* 响应式设计 */
@media (max-width: 768px) {
  /* 移动端消息列表整体优化 */
  .chat-messages {
    padding: 4px 8px; /* 大幅减少内边距 */
    font-size: 0.8rem; /* 整体字体缩小 */
  }
  
  /* 移动端消息行间距优化 */
  .message-row { 
    margin: 2px 0; /* 大幅减少消息间距 */
  }
  
  /* 移动端用户消息占更多空间但留出边距 */
  .message-row[justify="end"] .message-col {
    width: 85% !important; /* 用户消息在移动端占85% */
  }
  
  /* 移动端AI消息占更多空间 */
  .message-row[justify="start"] .message-col {
    width: 92% !important; /* AI消息在移动端占92% */
  }
  
  /* 有文件预览时的移动端优化 */
  .chat-messages.with-file-preview .message-row[justify="end"] .message-col {
    width: 80% !important; /* 文件预览时用户消息占80% */
  }
  
  .chat-messages.with-file-preview .message-row[justify="start"] .message-col {
    width: 90% !important; /* AI消息占90% */
  }
}
</style>