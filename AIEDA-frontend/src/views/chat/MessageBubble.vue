<!-- MessageBubble.vue -->
<script setup lang="ts">
import { ElCard, ElCollapse, ElCollapseItem } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { watch, ref, computed } from 'vue'

// 接收单个消息参数
const props = defineProps<{
  content: string
  isUser: boolean // true=用户消息，false=AI消息
  isStreaming?: boolean // 是否正在流式输出
  isError?: boolean // 是否为错误消息
}>()

const md = new MarkdownIt()
const activeCollapseItems = ref<string[]>([])

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

// 处理内容，将工具调用和引用标签转换为折叠组件
const processContent = (text: string) => {
  if (!text) return { mainContent: '', toolCalls: [], references: [] }
  
  let processed = text
  const toolCalls: Array<{id: string, name: string, content: string}> = []
  const references: Array<{id: string, tagName: string, link: string, index: string, text: string, refId: number}> = []
  let refCounter = 1 // 按顺序增长的ref_id计数器
  
  // 处理工具调用标签（如 ```tool\n调用`mcp_client`\noperation: call_tool, arguments: {'query': '王力宏'}, tool_name: bing_search\n```）
  processed = processed.replace(/```tool\n([\s\S]*?)\n```/g, (_, toolContent) => {
    const toolId = `tool-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    // 从工具内容中提取工具名称
    const toolNameMatch = toolContent.match(/调用`([^`]+)`/)
    const toolName = toolNameMatch ? toolNameMatch[1] : 'unknown_tool'
    
    toolCalls.push({
      id: toolId,
      name: toolName,
      content: toolContent.trim()
    })
    return `[🔧 工具调用: ${toolName}]`
  })
  
  // 处理普通工具调用标签 <tool_name>content</tool_name>（保留原有功能）
  processed = processed.replace(/<(\w+)>([\s\S]*?)<\/\1>/g, (_, toolName, toolContent) => {
    // 跳过引用类型的标签（有link和index属性的）
    if (toolContent.includes('link=') && toolContent.includes('index=')) {
      return `<${toolName}>${toolContent}</${toolName}>`
    }
    
    const toolId = `tool-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    toolCalls.push({
      id: toolId,
      name: toolName,
      content: toolContent.trim()
    })
    return `[🔧 工具调用: ${toolName}]`
  })
  
  // 处理引用标签 <tag_name link="..." index="...">text</tag_name>
  // 支持任意标签名，不仅限于mcreference
  processed = processed.replace(/<(\w+)\s+link="([^"]*?)"\s+index="([^"]*?)"[^>]*>([\s\S]*?)<\/\1>/g, (_, tagName, link, index, text) => {
    const elementId = `ref-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    references.push({
      id: elementId,
      tagName: tagName, // 保存原始标签名
      link: decodeURIComponent(link),
      index,
      text,
      refId: refCounter++ // 按顺序增长的ref_id
    })
    return `[📚 ${tagName}_${index}]`
  })
  
  // 处理旧的ref标签
  processed = processed.replace(/<ref>\[(.*?)\]<\/ref>/g, '[$1]')
  // 移除answer标签但保留内容
  processed = processed.replace(/<answer>([\s\S]*?)<\/answer>/g, '$1')
  
  return {
    mainContent: processed,
    toolCalls,
    references
  }
}

// 计算处理后的内容
const processedContent = computed(() => {
  return processContent(props.content)
})
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
      <!-- 主要内容 -->
      <div class="main-content">
        <!-- 对于正在流式输出的内容，先显示原始文本，流式完成后再渲染markdown -->
        <template v-if="props.isStreaming">
          <!-- 流式输出时使用简单文本渲染，避免频繁的markdown解析影响性能 -->
          <div class="streaming-content">{{ processedContent.mainContent }}</div>
        </template>
        <template v-else>
          <!-- 流式完成后渲染markdown格式 -->
          <div class="md-content" v-html="md.render(processedContent.mainContent)" />
        </template>
        
        <!-- 流式输出指示器 -->
        <div v-if="props.isStreaming && !props.content.includes('🤔') && !props.content.includes('⏳') && !props.content.includes('❌')" class="streaming-indicator">
          <span class="cursor">|</span>
        </div>
      </div>

      <!-- 工具调用折叠区域 -->
      <div v-if="processedContent.toolCalls.length > 0" class="tool-calls-section">
        <el-collapse v-model="activeCollapseItems" accordion>
          <el-collapse-item 
            v-for="tool in processedContent.toolCalls" 
            :key="tool.id"
            :title="`🔧 工具调用: ${tool.name}`"
            :name="tool.id"
            class="tool-collapse-item"
          >
            <div class="tool-content-wrapper">
              <div class="tool-content" v-html="md.render(tool.content)"></div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 引用链接折叠区域 -->
      <div v-if="processedContent.references.length > 0" class="references-section">
        <el-collapse v-model="activeCollapseItems" accordion>
          <el-collapse-item 
            v-for="ref in processedContent.references" 
            :key="ref.id"
            :title="`📚 ${ref.tagName} [ref_${ref.refId}]: ${ref.text}`"
            :name="ref.id"
            class="reference-collapse-item"
          >
            <div class="reference-content-wrapper">
              <div class="reference-info">
                <div class="reference-id">
                  <strong>Ref ID:</strong> ref_{{ ref.refId }}
                </div>
                <div class="reference-tag">
                  <strong>标签类型:</strong> {{ ref.tagName }}
                </div>
                <div class="reference-index">
                  <strong>索引:</strong> {{ ref.index }}
                </div>
              </div>
              <div class="reference-link">
                <strong>链接:</strong> 
                <a :href="ref.link" target="_blank" rel="noopener noreferrer" class="external-link">
                  {{ ref.link }}
                </a>
              </div>
              <div class="reference-text">
                <strong>引用文本:</strong> {{ ref.text }}
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
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

.main-content {
  margin-bottom: 8px;
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

/* 工具调用和引用区域样式 */
.tool-calls-section,
.references-section {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
}

.tool-calls-section:first-child,
.references-section:first-child {
  border-top: none;
  padding-top: 0;
  margin-top: 0;
}

/* 折叠组件样式 */
:deep(.el-collapse) {
  border: none;
  background: transparent;
}

:deep(.el-collapse-item__header) {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 0.9em;
  color: #495057;
  height: auto;
  line-height: 1.4;
  margin-bottom: 4px;
  transition: all 0.3s ease;
}

:deep(.el-collapse-item__header:hover) {
  background-color: #e9ecef;
  border-color: #dee2e6;
}

:deep(.el-collapse-item__header.is-active) {
  background-color: rgba(102, 8, 116, 0.08);
  border-color: rgba(102, 8, 116, 0.2);
  color: rgb(102, 8, 116);
}

:deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

:deep(.el-collapse-item__content) {
  padding: 8px 0 0 0;
  border: none;
  background: transparent;
}

/* 工具内容样式 */
.tool-content-wrapper {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  max-height: 300px;
  overflow: auto;
  resize: vertical;
  min-height: 100px;
}

.tool-content {
  padding: 12px;
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 0.85em;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.tool-content :deep(pre) {
  background-color: #ffffff;
  border: 1px solid #dee2e6;
  margin: 4px 0;
}

.tool-content :deep(code) {
  background-color: #ffffff;
  border: 1px solid #dee2e6;
}

/* 引用内容样式 */
.reference-content-wrapper {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 12px;
  max-height: 250px;
  overflow: auto;
  resize: vertical;
  min-height: 100px;
}

.reference-info {
  background-color: #e9ecef;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 0.85em;
}

.reference-info > div {
  margin-bottom: 4px;
}

.reference-info > div:last-child {
  margin-bottom: 0;
}

.reference-id {
  color: #495057;
  font-weight: 600;
}

.reference-tag {
  color: #007bff;
}

.reference-index {
  color: #28a745;
}

.reference-link {
  margin-bottom: 8px;
  word-break: break-all;
}

.reference-text {
  color: #6c757d;
  font-style: italic;
}

.external-link {
  color: rgb(102, 8, 116);
  text-decoration: none;
  font-size: 0.9em;
}

.external-link:hover {
  text-decoration: underline;
}

/* 工具和引用的特殊样式 */
.tool-collapse-item :deep(.el-collapse-item__header) {
  border-left: 3px solid #28a745;
}

.reference-collapse-item :deep(.el-collapse-item__header) {
  border-left: 3px solid #007bff;
}

/* 滚动条样式 */
.tool-content-wrapper::-webkit-scrollbar,
.reference-content-wrapper::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.tool-content-wrapper::-webkit-scrollbar-track,
.reference-content-wrapper::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.tool-content-wrapper::-webkit-scrollbar-thumb,
.reference-content-wrapper::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.tool-content-wrapper::-webkit-scrollbar-thumb:hover,
.reference-content-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>