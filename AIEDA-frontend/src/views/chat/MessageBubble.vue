<!-- MessageBubble.vue -->
<script setup lang="ts">
import { ElCard } from 'element-plus'
import MarkdownIt from 'markdown-it'
import texmath from 'markdown-it-texmath'
import katex from 'katex'
import { watch, computed, ref, nextTick } from 'vue'
import 'katex/dist/katex.min.css'

// 接收单个消息参数
const props = defineProps<{
  content: string
  isUser: boolean // true=用户消息，false=AI消息
  isStreaming?: boolean // 是否正在流式输出
  isError?: boolean // 是否为错误消息
}>()

// 响应式变量来控制气泡的最小高度
const bubbleMinHeight = ref('auto')

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
}).use(texmath, {
  engine: katex,
  delimiters: 'dollars',
  katexOptions: {
    throwOnError: false,
    errorColor: '#cc0000',
    // 添加一些常用的数学宏
    macros: {
      "\\RR": "\\mathbb{R}",
      "\\NN": "\\mathbb{N}",
      "\\ZZ": "\\mathbb{Z}",
      "\\QQ": "\\mathbb{Q}",
      "\\CC": "\\mathbb{C}",
      "\\FF": "\\mathbb{F}",
      "\\eps": "\\varepsilon",
      "\\veps": "\\varepsilon",
      "\\ph": "\\varphi",
      "\\vph": "\\varphi",
      "\\Om": "\\Omega",
      "\\om": "\\omega"
    }
  }
})

// 添加watch来调试props变化，同时优化性能
watch(() => props.content, (newContent, oldContent) => {
  // 当内容变化时，重置气泡高度
  bubbleMinHeight.value = 'auto'
  
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

// 处理内容，将工具调用和引用标签转换为内联标签
const processContent = (text: string) => {
  if (!text) return { processedText: '', toolCalls: [], references: [] }
  
  console.log('开始处理内容:', {
    originalLength: text.length,
    timestamp: new Date().toLocaleTimeString(),
    preview: text.substring(0, 200) + (text.length > 200 ? '...' : '')
  })
  
  let processed = text
  const toolCalls: Array<{id: string, name: string, content: string, position: number}> = []
  const references: Array<{id: string, tagName: string, link: string, index: string, text: string, refId: number, position: number}> = []
  let refCounter = 1 // 按顺序增长的ref_id计数器
  
  // 处理工具调用标签（如 ```tool\n调用`mcp_client`\noperation: call_tool, arguments: {'query': '王力宏'}, tool_name: bing_search\n```）
  processed = processed.replace(/```tool\n([\s\S]*?)\n```/g, (_, toolContent, offset) => {
    const toolId = `tool-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    // 从工具内容中提取工具名称
    const toolNameMatch = toolContent.match(/调用`([^`]+)`/)
    const toolName = toolNameMatch ? toolNameMatch[1] : 'unknown_tool'
    
    toolCalls.push({
      id: toolId,
      name: toolName,
      content: toolContent.trim(),
      position: offset
    })
    
    // 返回带有容器的HTML结构
    return `<span class="tag-container" data-tool-id="${toolId}">
      <span class="inline-tag tool-tag" data-tool-id="${toolId}" title="点击查看详细信息">🔧 ${toolName}</span>
      <div class="tag-expanded-content" data-for="${toolId}" style="display: none;">
        <pre class="tool-content">${toolContent.trim()}</pre>
      </div>
    </span>`
  })
  
  // 处理普通工具调用标签 <tool_name>content</tool_name>（保留原有功能）
  // 修改正则表达式以支持带点号的标签名，如 <default_api.command_executor>
  processed = processed.replace(/<([a-zA-Z_][a-zA-Z0-9_.]*?)>([\s\S]*?)<\/\1>/g, (match, toolName, toolContent, offset) => {
    // 跳过引用类型的标签（有link和index属性的）
    if (toolContent.includes('link=') && toolContent.includes('index=')) {
      return match
    }
    
    console.log('检测到工具调用标签:', {
      toolName,
      contentLength: toolContent.length,
      offset
    })
    
    const toolId = `tool-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    toolCalls.push({
      id: toolId,
      name: toolName,
      content: toolContent.trim(),
      position: offset
    })
    
    // 返回带有容器的HTML结构
    return `<span class="tag-container" data-tool-id="${toolId}">
      <span class="inline-tag tool-tag" data-tool-id="${toolId}" title="点击查看详细信息">🔧 ${toolName}</span>
      <div class="tag-expanded-content" data-for="${toolId}" style="display: none;">
        <pre class="tool-content">${toolContent.trim()}</pre>
      </div>
    </span>`
  })
  
  // 处理引用标签 <tag_name link="..." index="...">text</tag_name>
  // 支持任意标签名，不仅限于mcreference，也支持带点号的标签名
  processed = processed.replace(/<([a-zA-Z_][a-zA-Z0-9_.]*?)\s+link="([^"]*?)"\s+index="([^"]*?)"[^>]*>([\s\S]*?)<\/\1>/g, (_, tagName, link, index, text, offset) => {
    console.log('检测到引用标签:', {
      tagName,
      link,
      index,
      textLength: text.length,
      offset
    })
    
    const elementId = `ref-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    const currentRefId = refCounter++
    references.push({
      id: elementId,
      tagName: tagName, // 保存原始标签名
      link: decodeURIComponent(link),
      index,
      text,
      refId: currentRefId,
      position: offset
    })
    
    // 返回带有容器的HTML结构
    return `<span class="tag-container" data-ref-id="${elementId}">
      <span class="inline-tag reference-tag" data-ref-id="${elementId}" title="点击查看详细信息">📚 ${tagName}_${index}</span>
      <div class="tag-expanded-content" data-for="${elementId}" style="display: none;">
        <div class="reference-info">
          <div class="reference-id"><strong>Ref ID:</strong> ref_${currentRefId}</div>
          <div class="reference-tag"><strong>标签类型:</strong> ${tagName}</div>
          <div class="reference-index"><strong>索引:</strong> ${index}</div>
        </div>
        <div class="reference-link">
          <strong>链接:</strong> 
          <a href="${decodeURIComponent(link)}" target="_blank" rel="noopener noreferrer" class="external-link">
            ${decodeURIComponent(link)}
          </a>
        </div>
        <div class="reference-text">
          <strong>引用文本:</strong> 
          <div class="reference-text-content">${text}</div>
        </div>
      </div>
    </span>`
  })
  
  // 处理旧的ref标签
  processed = processed.replace(/<ref>\[(.*?)\]<\/ref>/g, '[$1]')
  // 移除answer标签但保留内容
  processed = processed.replace(/<answer>([\s\S]*?)<\/answer>/g, '$1')
  
  console.log('内容处理完成:', {
    originalLength: text.length,
    processedLength: processed.length,
    toolCallsCount: toolCalls.length,
    referencesCount: references.length,
    timestamp: new Date().toLocaleTimeString()
  })
  
  return {
    processedText: processed,
    toolCalls,
    references
  }
}

// 计算处理后的内容
const processedContent = computed(() => {
  return processContent(props.content)
})

// 添加点击处理函数
const handleTagClick = async (event: Event) => {
  const target = event.target as HTMLElement
  
  console.log('标签点击事件:', {
    target: target.className,
    isInlineTag: target.classList.contains('inline-tag'),
    isStreaming: props.isStreaming,
    timestamp: new Date().toLocaleTimeString()
  })
  
  if (target.classList.contains('inline-tag')) {
    const toolId = target.getAttribute('data-tool-id')
    const refId = target.getAttribute('data-ref-id')
    
    console.log('标签属性:', {
      toolId,
      refId,
      parentElement: target.parentElement?.className
    })
    
    // 找到对应的展开内容
    let expandedContent: HTMLElement | null = null
    
    if (toolId) {
      expandedContent = target.parentElement?.querySelector(`[data-for="${toolId}"]`) as HTMLElement
    } else if (refId) {
      expandedContent = target.parentElement?.querySelector(`[data-for="${refId}"]`) as HTMLElement
    }
    
    console.log('展开内容查找结果:', {
      expandedContent: expandedContent ? '找到' : '未找到',
      currentDisplay: expandedContent?.style.display,
      expandedContentClass: expandedContent?.className
    })
    
    if (expandedContent) {
      // 切换显示状态
      const isCurrentlyHidden = expandedContent.style.display === 'none' || expandedContent.style.display === ''
      expandedContent.style.display = isCurrentlyHidden ? 'block' : 'none'
      
      console.log('切换展开状态:', {
        wasHidden: isCurrentlyHidden,
        newDisplay: expandedContent.style.display,
        timestamp: new Date().toLocaleTimeString()
      })
      
      // 如果是展开操作，检查是否需要扩展气泡高度
      if (isCurrentlyHidden) {
        await nextTick() // 等待DOM更新
        checkAndAdjustBubbleHeight(expandedContent, target)
      } else {
        // 如果是收起操作，重置气泡高度
        bubbleMinHeight.value = 'auto'
      }
    }
  }
}

// 检查并调整气泡高度的函数
const checkAndAdjustBubbleHeight = (expandedContent: HTMLElement, triggerElement: HTMLElement) => {
  try {
    // 获取气泡容器元素
    const bubbleElement = triggerElement.closest('.el-card')
    if (!bubbleElement) {
      console.warn('未找到气泡容器元素')
      return
    }
    
    // 等待一小段时间确保样式已应用
    setTimeout(() => {
      // 获取当前气泡的边界
      const bubbleRect = bubbleElement.getBoundingClientRect()
      const expandedRect = expandedContent.getBoundingClientRect()
      
      console.log('气泡和展开内容位置信息:', {
        bubbleBottom: bubbleRect.bottom,
        expandedBottom: expandedRect.bottom,
        bubbleHeight: bubbleRect.height,
        expandedHeight: expandedRect.height,
        needsExtension: expandedRect.bottom > bubbleRect.bottom
      })
      
      // 如果展开内容的底部超出了气泡的底部
      if (expandedRect.bottom > bubbleRect.bottom) {
        const additionalHeight = expandedRect.bottom - bubbleRect.bottom + 24 // 额外添加24px的缓冲空间
        const newMinHeight = bubbleRect.height + additionalHeight
        
        console.log('需要扩展气泡高度:', {
          originalHeight: bubbleRect.height,
          additionalHeight,
          newMinHeight
        })
        
        bubbleMinHeight.value = `${newMinHeight}px`
      }
    }, 50) // 50ms的延迟确保DOM完全更新
  } catch (error) {
    console.error('检查气泡高度时发生错误:', error)
  }
}
</script>

<template>
  <el-card 
    :class="[
      props.isUser ? 'user-message' : 'ai-message',
      { 'streaming-message': props.isStreaming, 'error-message': props.isError }
    ]"
    shadow="never"
    body-style="padding:12px 16px; display: block; max-width: 100%; word-wrap: break-word;"
    :style="{ minHeight: bubbleMinHeight }"
  >
    <!-- 用户消息 -->
    <template v-if="props.isUser">
      {{ props.content }}
    </template>

    <!-- AI消息 -->
    <template v-else>
      <!-- 主要内容 -->
      <div 
        class="main-content message-content" 
        :data-streaming="props.isStreaming"
        @click="handleTagClick"
      >
        <!-- 对于正在流式输出的内容，也需要渲染HTML标签 -->
        <template v-if="props.isStreaming">
          <!-- 流式输出时也渲染HTML，但不进行markdown处理以提升性能 -->
          <div class="streaming-content" v-html="processedContent.processedText"></div>
        </template>
        <template v-else>
          <!-- 流式完成后渲染markdown格式 -->
          <div class="md-content" v-html="md.render(processedContent.processedText)" />
        </template>
        
        <!-- 流式输出指示器 -->
        <div v-if="props.isStreaming && !props.content.includes('🤔') && !props.content.includes('⏳') && !props.content.includes('❌')" class="streaming-indicator">
          <span class="cursor">|</span>
        </div>
      </div>
    </template>
  </el-card>
</template>

<style scoped>
.user-message {
  background-color: rgba(102, 8, 116, 0.08);
  border: 1px solid rgba(102, 8, 116, 0.2);
  border-radius: 12px;
  transition: min-height 0.3s ease-out;
  width: 100%;
  word-wrap: break-word;
  box-sizing: border-box;
}

.ai-message {
  background-color: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  transition: min-height 0.3s ease-out;
  width: 100%;
  word-wrap: break-word;
  box-sizing: border-box;
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
  line-height: 1.6;
  max-width: 100%;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  box-sizing: border-box;
  width: 100%;
}

.streaming-content {
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  line-height: 1.6;
  font-family: inherit;
  max-width: 100%;
  box-sizing: border-box;
  width: 100%;
}

/* 确保md-content也受宽度限制 */
.md-content {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.md-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 8px;
  margin: 8px 0;
  border: 1px solid #e8e8e8;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow-x: auto;
}

.md-content :deep(code) {
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 0.9em;
  background-color: #f0f0f0;
  padding: 2px 4px;
  border-radius: 3px;
  word-break: break-all;
  white-space: pre-wrap;
}

.md-content :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.md-content :deep(ul), .md-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.md-content :deep(li) {
  margin: 4px 0;
  word-wrap: break-word;
  word-break: break-word;
}

/* 确保所有块级元素都受宽度限制 */
.md-content :deep(div),
.md-content :deep(blockquote),
.md-content :deep(table),
.md-content :deep(h1),
.md-content :deep(h2),
.md-content :deep(h3),
.md-content :deep(h4),
.md-content :deep(h5),
.md-content :deep(h6) {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  word-wrap: break-word;
  word-break: break-word;
}

/* KaTeX数学公式样式优化 */
.md-content :deep(.katex) {
  font-size: 1.1em;
  line-height: 1.6;
  font-family: 'KaTeX_Main', 'Computer Modern', 'Times New Roman', serif;
  color: #2c3e50;
}

/* 块级数学公式（居中显示） */
.md-content :deep(.katex-display) {
  margin: 20px 0;
  text-align: center;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 16px;
  background-color: #f9f9f9;
  border: 1px solid #e1e8ed;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.md-content :deep(.katex-display > .katex) {
  display: inline-block;
  white-space: nowrap;
  max-width: 100%;
  font-size: 1.2em;
}

/* 行内数学公式 */
.md-content :deep(.katex-inline) {
  background-color: rgba(102, 8, 116, 0.05);
  padding: 2px 4px;
  border-radius: 3px;
  border: 1px solid rgba(102, 8, 116, 0.1);
}

/* 防止公式溢出 */
.md-content :deep(.katex .mord) {
  margin-right: 0.05em;
}

.md-content :deep(.katex .mbin) {
  margin: 0 0.22em;
}

.md-content :deep(.katex .mrel) {
  margin: 0 0.27em;
}

.md-content :deep(.katex .mpunct) {
  margin-right: 0.1em;
}

/* 特殊数学符号样式 */
.md-content :deep(.katex .mopen),
.md-content :deep(.katex .mclose) {
  color: #e74c3c;
  font-weight: 600;
}

/* 函数名样式 */
.md-content :deep(.katex .mop) {
  color: #3498db;
  font-weight: 500;
}

/* 希腊字母样式 */
.md-content :deep(.katex .mord.mathnormal) {
  color: #8e44ad;
  font-style: italic;
}

/* 分数线样式 */
.md-content :deep(.katex .frac-line) {
  border-bottom-color: #34495e;
  border-bottom-width: 0.05em;
}

/* 矩阵和向量样式 */
.md-content :deep(.katex .arraycolsep) {
  width: 0.5em;
}

.md-content :deep(.katex .begin-equation) {
  margin: 16px 0;
}

/* 根号样式 */
.md-content :deep(.katex .sqrt > .vlist-t) {
  border-left-color: #2c3e50;
}

/* 错误显示样式 */
.md-content :deep(.katex-error) {
  color: #cc0000;
  background-color: #ffebee;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #ffcdd2;
  font-family: 'Courier New', monospace;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .md-content :deep(.katex-display) {
    font-size: 0.9em;
    padding: 12px;
    margin: 16px 0;
  }
  
  .md-content :deep(.katex) {
    font-size: 1em;
  }
  
  .md-content :deep(.katex-display > .katex) {
    font-size: 1.1em;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .md-content :deep(.katex) {
    color: #ecf0f1;
  }
  
  .md-content :deep(.katex-display) {
    background-color: #2c3e50;
    border-color: #34495e;
  }
  
  .md-content :deep(.katex-inline) {
    background-color: rgba(102, 8, 116, 0.15);
    border-color: rgba(102, 8, 116, 0.3);
  }
  
  .md-content :deep(.katex-error) {
    background-color: #3c2415;
    border-color: #8b4513;
    color: #ff6b6b;
  }
}

/* 标签容器样式 - 支持内联展开 */
:deep(.tag-container) {
  position: relative;
  display: inline-block;
  margin: 0 1px; /* 调整这个值可以控制标签间距：0px = 最紧凑，2px = 稍宽松 */
  vertical-align: baseline;
  height: auto;
  min-height: 0;
  max-height: none;
  overflow: visible;
  line-height: normal;
}

/* 内联标签样式 - 更紧凑的设计 */
:deep(.inline-tag) {
  display: inline-block;
  padding: 0 3px; /* 调整这个值可以控制标签大小：0 2px = 最小，2px 6px = 较大 */
  margin: 0;
  border-radius: 6px; /* 调整这个值可以控制圆角：4px = 较小圆角，8px = 较大圆角 */
  font-size: 0.7em; /* 调整这个值可以控制字体大小：0.6em = 更小，0.8em = 更大 */
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  white-space: nowrap;
  user-select: none;
  vertical-align: baseline;
  line-height: 1.1; /* 调整这个值可以控制行高：1.0 = 最紧凑，1.3 = 较松 */
  height: auto;
  min-height: 0;
  max-height: none;
}

:deep(.tool-tag) {
  background-color: rgba(40, 167, 69, 0.1);
  color: #28a745;
  border: 1px solid rgba(40, 167, 69, 0.3);
}

:deep(.tool-tag:hover) {
  background-color: rgba(40, 167, 69, 0.2);
  border-color: rgba(40, 167, 69, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(40, 167, 69, 0.2);
}

:deep(.reference-tag) {
  background-color: rgba(0, 123, 255, 0.1);
  color: #007bff;
  border: 1px solid rgba(0, 123, 255, 0.3);
}

:deep(.reference-tag:hover) {
  background-color: rgba(0, 123, 255, 0.2);
  border-color: rgba(0, 123, 255, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 123, 255, 0.2);
}

/* 内联展开内容区域样式 */
.inline-expandable-content {
  display: inline-block;
  width: 100%;
  margin: 4px 0;
}

/* 展开内容区域样式 */
.expandable-content {
  margin-top: 12px;
}

.expanded-item {
  margin-bottom: 8px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: slideDown 0.3s ease-out;
}

/* 展开内容容器 - 修复流式输出时的显示问题 */
:deep(.tag-expanded-content) {
  position: absolute;
  top: 100%;
  left: 0;
  width: 400px;
  min-width: 400px;
  max-width: 500px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  animation: slideDown 0.2s ease-out;
  margin-top: 2px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

/* 工具调用展开内容的左边框 */
:deep([data-tool-id] .tag-expanded-content) {
  border-left: 4px solid #28a745;
}

/* 引用展开内容的左边框 */
:deep([data-ref-id] .tag-expanded-content) {
  border-left: 4px solid #007bff;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.tool-content {
  white-space: pre-wrap !important;
  word-wrap: break-word !important;
  word-break: break-word !important;
  overflow-wrap: break-word !important;
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #495057;
  background: #f8f9fa;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #e9ecef;
  margin: 0;
  overflow-x: auto;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.reference-info {
  font-size: 13px;
  line-height: 1.5;
  color: #495057;
  width: 100%;
  margin-bottom: 12px;
}

.reference-info > div {
  margin-bottom: 8px;
  word-wrap: break-word;
  word-break: break-word;
  width: 100%;
}

.reference-info > div:last-child {
  margin-bottom: 0;
}

.reference-id {
  font-weight: 600;
  color: #007bff;
}

.reference-tag {
  color: #007bff;
  font-weight: 500;
}

.reference-index {
  color: #28a745;
  font-weight: 500;
}

.reference-link {
  color: #6c757d;
  font-size: 12px;
  word-break: break-all;
  margin-bottom: 8px;
  width: 100%;
}

.reference-text {
  background: #f8f9fa;
  padding: 8px;
  border-radius: 4px;
  border-left: 3px solid #007bff;
  margin-top: 8px;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-word;
  line-height: 1.5;
  width: 100%;
  box-sizing: border-box;
}

.reference-text-content {
  font-size: 13px;
  color: #495057;
  width: 100%;
  word-wrap: break-word;
  word-break: break-word;
}

.external-link {
  color: rgb(102, 8, 116);
  text-decoration: none;
}

.external-link:hover {
  text-decoration: underline;
}

/* 滚动条样式 */
.expanded-content::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.expanded-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.expanded-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.expanded-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 确保 pre 标签内的内容也能正确换行 */
:deep(.tool-content pre),
:deep(pre.tool-content) {
  white-space: pre-wrap !important;
  word-wrap: break-word !important;
  word-break: break-word !important;
  overflow-wrap: break-word !important;
  max-width: 100% !important;
  overflow-x: auto !important;
}

.tool-content::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.tool-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.tool-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.tool-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.tool-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.tool-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.tool-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>