<!-- MessageBubble.vue -->
<script setup lang="ts">
import { ElCard } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { watch } from 'vue'

// 接收单个消息参数
const props = defineProps<{
  content: string
  isUser: boolean // true=用户消息，false=AI消息
  isStreaming?: boolean // 是否正在流式输出
  isError?: boolean // 是否为错误消息
}>()

const md = new MarkdownIt()

// 添加watch来调试props变化，同时优化性能
watch(() => props.content, (newContent, oldContent) => {
  if (!props.isUser) {
    console.log('MessageBubble content 更新:', {
      old: oldContent?.substring(0, 30) + '...',
      new: newContent?.substring(0, 30) + '...',
      length: newContent?.length,
      isStreaming: props.isStreaming,
      fullContent: newContent, // 添加完整内容用于调试
      timestamp: new Date().toLocaleTimeString()
    })
  }
}, { immediate: true })

// 添加对isStreaming的监听，优化渲染时机
watch(() => props.isStreaming, (newStreaming, oldStreaming) => {
  if (!props.isUser) {
    console.log('MessageBubble isStreaming 更新:', {
      old: oldStreaming,
      new: newStreaming,
      contentLength: props.content?.length,
      timestamp: new Date().toLocaleTimeString()
    })
  }
})

const replaceRefTags = (text: string) => {
  if (!text) return ''
  // 处理引用标签
  let processed = text.replace(/<ref>\[(.*?)\]<\/ref>/g, '[$1]')
  // 移除answer标签但保留内容
  processed = processed.replace(/<answer>([\s\S]*?)<\/answer>/g, '$1')
  return processed
}
</script>

<template>
  <el-card 
    :class="[
      props.isUser ? 'user-message' : 'ai-message',
      { 'streaming-message': props.isStreaming, 'error-message': props.isError }
    ]"
    shadow="never"
    body-style="padding:12px 16px; display: inline-block"
  >
    <!-- 用户消息 -->
    <template v-if="props.isUser">
      {{ props.content }}
    </template>

    <!-- AI消息 -->
    <template v-else>
      <!-- 对于正在流式输出的内容，先显示原始文本，流式完成后再渲染markdown -->
      <template v-if="props.isStreaming">
        <!-- 流式输出时使用简单文本渲染，避免频繁的markdown解析影响性能 -->
        <div class="streaming-content">{{ replaceRefTags(props.content) }}</div>
      </template>
      <template v-else>
        <!-- 流式完成后渲染markdown格式 -->
        <div class="md-content" v-html="md.render(replaceRefTags(props.content))" />
      </template>
      
      <!-- 流式输出指示器 -->
      <div v-if="props.isStreaming && !props.content.includes('🤔') && !props.content.includes('⏳') && !props.content.includes('❌')" class="streaming-indicator">
        <span class="cursor">|</span>
      </div>
    </template>
  </el-card>
</template>

<style scoped>
.user-message {
  background-color: rgba(102, 8, 116, 0.08);
  border: 1px solid rgba(102, 8, 116, 0.2);
  border-radius: 12px;
}

.ai-message {
  background-color: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
}

.streaming-message {
  border-color: rgba(102, 8, 116, 0.3) !important;
  box-shadow: 0 0 0 1px rgba(102, 8, 116, 0.1) !important;
  position: relative;
}

.streaming-message::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 12px;
  background: linear-gradient(45deg, transparent 30%, rgba(102, 8, 116, 0.05) 50%, transparent 70%);
  animation: shimmer 2s infinite;
  pointer-events: none;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.error-message {
  border-color: #f56565 !important;
  background-color: #fef5f5 !important;
}

.streaming-indicator {
  display: inline-block;
  margin-left: 4px;
}

.cursor {
  animation: blink 1s infinite;
  font-weight: bold;
  color: rgba(102, 8, 116, 0.8);
  font-size: 1.2em;
  margin-left: 2px;
}

@keyframes blink {
  0%, 45% { opacity: 1; }
  50%, 100% { opacity: 0; }
}

.thought-bubble {
  background-color: #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
  font-size: 0.85em;
  color: #666;
  border-left: 3px solid #ccc;
  display: none; /* 隐藏thought相关样式 */
}

.streaming-content {
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.6;
  font-family: inherit;
}

.md-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 8px;
  margin: 8px 0;
  border: 1px solid #e8e8e8;
}

.md-content :deep(code) {
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 0.9em;
  background-color: #f0f0f0;
  padding: 2px 4px;
  border-radius: 3px;
}

.md-content :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
}

.md-content :deep(ul), .md-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.md-content :deep(li) {
  margin: 4px 0;
}
</style>