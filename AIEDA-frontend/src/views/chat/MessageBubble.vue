<!-- MessageBubble.vue -->
<script setup lang="ts">
import { ElCard } from 'element-plus'
import MarkdownIt from 'markdown-it'

// 接收单个消息参数
const props = defineProps<{
  content: string
  isUser: boolean // true=用户消息，false=AI消息
}>()

const md = new MarkdownIt()

// 使用props防止TypeScript警告
const { content, isUser } = props

// 解析内容
const parseThoughtContent = (content: string) => {
  const match = content.match(/<thought>([\s\S]*?)<\/thought>/)
  return match ? match[1].trim().replace(/\n/g, '<br>') : ''
}

const parseResponseContent = (content: string) => {
  return content.replace(/<thought>[\s\S]*?<\/thought>/, '').trim()
}

const replaceRefTags = (text: string) => text.replace(/<ref>\[(.*?)\]<\/ref>/g, '[$1]')
</script>

<template>
  <el-card 
    :class="isUser ? 'user-message' : 'ai-message'"
    shadow="never"
    body-style="padding:12px 16px; display: inline-block"
  >
    <!-- 用户消息 -->
    <template v-if="isUser">
      {{ content }}
    </template>

    <!-- AI消息（带思考过程） -->
    <template v-else>
      <div v-if="content.includes('<thought>')" class="ai-content">
        <!-- 思考过程 -->
        <div class="thought-bubble">
          <div v-html="parseThoughtContent(content)"></div>
        </div>
        <!-- 正式回复 -->
        <div 
          class="md-content" 
          v-html="md.render(replaceRefTags(parseResponseContent(content)))"
        />
      </div>
      <div v-else class="md-content" v-html="md.render(replaceRefTags(content))" />
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

.thought-bubble {
  background-color: #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
  font-size: 0.85em;
  color: #666;
  border-left: 3px solid #ccc;
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