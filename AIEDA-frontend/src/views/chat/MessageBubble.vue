<!-- MessageBubble.vue -->
<script setup lang="ts">
import { ElCard } from 'element-plus'
import MarkdownIt from 'markdown-it'
import texmath from 'markdown-it-texmath'
import katex from 'katex'
import { watch, computed, ref, nextTick, onMounted } from 'vue'
import 'katex/dist/katex.min.css'
import type { FileVO } from '../../api/file'
import { getFilesByRecordId, downloadFile } from '../../api/file'
import MessageFileList from '../../components/File/MessageFileList.vue'
import LLMUserConfirmation from '../../components/LLM/LLMUserConfirmation.vue'

// 接收单个消息参数
const props = defineProps<{
  content: string
  isUser: boolean // true=用户消息，false=AI消息
  isStreaming?: boolean // 是否正在流式输出
  isError?: boolean // 是否为错误消息
  isPaused?: boolean // 是否处于暂停状态
  recordId?: number // 消息记录ID，用于获取关联的文件
  attachedFiles?: FileVO[] // 新增：直接传入的附件文件列表（用于刚发送的消息）
  uid?: number // 用户ID，用于发送确认信息
  sid?: number // 会话ID，用于发送确认信息
}>()

// 定义事件
const emit = defineEmits<{
  'open-file-preview': [file: FileVO] // 文件预览事件
  'send-confirmation': [choice: '1' | '2'] // 用户确认事件
}>()

// 响应式变量来控制气泡的最小高度
const bubbleMinHeight = ref('auto')
const associatedFiles = ref<FileVO[]>([]) // 关联的文件列表
const forceUpdateKey = ref(0) // 强制更新键

// 计算最终要显示的文件列表
const displayFiles = computed(() => {
  // 如果有直接传入的附件文件，优先使用（用于刚发送的消息）
  if (props.attachedFiles && props.attachedFiles.length > 0) {
    return props.attachedFiles
  }
  // 否则使用从后端加载的关联文件
  return associatedFiles.value
})

// 在组件挂载时加载关联的文件
onMounted(() => {
  if (props.recordId && props.isUser && !props.attachedFiles) {
    loadAssociatedFiles()
  }
})

// 监听recordId变化，重新加载文件
watch(() => props.recordId, (newRecordId) => {
  if (newRecordId && props.isUser && !props.attachedFiles) {
    loadAssociatedFiles()
  } else if (!props.attachedFiles) {
    associatedFiles.value = []
  }
})

// 加载关联的文件列表
const loadAssociatedFiles = async () => {
  if (!props.recordId) return
  
  try {
    console.log('加载消息关联文件:', { recordId: props.recordId })
    const response = await getFilesByRecordId(props.recordId)
    
    if (response.data && response.data.code === '200') {
      associatedFiles.value = response.data.data.files || []
      console.log('关联文件加载成功:', associatedFiles.value)
    } else {
      associatedFiles.value = []
    }
  } catch (error) {
    console.error('加载关联文件失败:', error)
    associatedFiles.value = []
  }
}

// 处理文件预览点击
const handleFilePreview = (file: FileVO) => {
  console.log('预览关联文件:', file.originalName)
  emit('open-file-preview', file)
}

// 处理文件下载
const handleFileDownload = async (file: FileVO) => {
  try {
    await downloadFile(file.fileId, file.originalName)
    console.log('文件下载成功:', file.originalName)
  } catch (error) {
    console.error('文件下载失败:', error)
  }
}

// 处理用户确认选择
const handleUserConfirmation = (choice: '1' | '2') => {
  console.log('用户确认选择:', choice)
  emit('send-confirmation', choice)
}

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

// 添加watch来调试props变化，同时优化性能和强制触发更新
watch(() => props.content, (newContent, oldContent) => {
  // 当内容变化时，重置气泡高度
  bubbleMinHeight.value = 'auto'
  
  // 强制更新计算属性
  forceUpdateKey.value++
  
  if (!props.isUser) {
    console.log('MessageBubble content 更新:', {
      old: oldContent?.substring(0, 30) + '...',
      new: newContent?.substring(0, 30) + '...',
      length: newContent?.length,
      isStreaming: props.isStreaming,
      forceUpdateKey: forceUpdateKey.value,
      timestamp: new Date().toLocaleTimeString()
    })
    
    // 强制触发重新渲染，确保markdown内容及时更新
    nextTick(() => {
      console.log('MessageBubble DOM 更新完成，内容已刷新')
    })
  }
}, { immediate: true, flush: 'post' }) // 使用post flush确保DOM更新后执行

// 添加对isStreaming的监听，优化渲染时机
watch(() => props.isStreaming, (newStreaming, oldStreaming) => {
  // 强制更新计算属性
  forceUpdateKey.value++
  
  if (!props.isUser) {
    console.log('MessageBubble isStreaming 更新:', {
      old: oldStreaming,
      new: newStreaming,
      contentLength: props.content?.length,
      forceUpdateKey: forceUpdateKey.value,
      timestamp: new Date().toLocaleTimeString()
    })
    
    // 当流式状态改变时，强制重新渲染
    nextTick(() => {
      console.log('MessageBubble 流式状态变化后强制重新渲染')
    })
  }
}, { flush: 'post' })

// 处理内容，将工具调用和引用标签转换为内联标签
const processContent = (text: string) => {
  if (!text) return { processedText: '', toolCalls: [], references: [], userConfirmation: null }
  
  console.log('开始处理内容:', {
    originalLength: text.length,
    timestamp: new Date().toLocaleTimeString(),
    preview: text.substring(0, 200) + (text.length > 200 ? '...' : '')
  })
  
  let processed = text
  const toolCalls: Array<{id: string, name: string, content: string, position: number}> = []
  const references: Array<{id: string, tagName: string, link: string, index: string, text: string, refId: number, position: number}> = []
  let refCounter = 1 // 按顺序增长的ref_id计数器
  let userConfirmation: { message: string } | null = null
  
  // 调试：检查是否包含user_confirmation标签
  const hasUserConfirmation = processed.includes('<user_confirmation>')
  console.log('调试信息 - 检查user_confirmation:', {
    hasTag: hasUserConfirmation,
    contentPreview: processed.substring(0, 500),
    timestamp: new Date().toLocaleTimeString()
  })
  
  // 检测用户确认工具调用，匹配 <user_confirmation>任何内容</user_confirmation>
  const userConfirmationMatch = processed.match(/<user_confirmation>([\s\S]*?)<\/user_confirmation>/)
  if (userConfirmationMatch) {
    console.log('匹配到user_confirmation标签:', {
      fullMatch: userConfirmationMatch[0],
      innerContent: userConfirmationMatch[1]
    })
    
    try {
      let content = userConfirmationMatch[1].trim()
      
      // 尝试提取JSON内容，支持多种格式
      let jsonString = content
      
      // 如果包含代码块，提取代码块内容
      const codeBlockMatch = content.match(/```(?:json)?\s*([\s\S]*?)\s*```/)
      if (codeBlockMatch) {
        console.log('检测到代码块格式')
        jsonString = codeBlockMatch[1].trim()
      }
      
      console.log('准备解析JSON:', jsonString)
      
      // 处理单引号JSON格式，将单引号替换为双引号
      let normalizedJsonString = jsonString
      try {
        // 尝试直接解析
        JSON.parse(normalizedJsonString)
      } catch (e) {
        // 如果解析失败，尝试将单引号替换为双引号
        console.log('JSON解析失败，尝试转换单引号为双引号')
        normalizedJsonString = jsonString.replace(/'/g, '"')
      }
      
      const confirmationData = JSON.parse(normalizedJsonString)
      
      if (confirmationData.message) {
    
        userConfirmation = { message: confirmationData.message }
        console.log('检测到用户确认请求:', userConfirmation)
        
        // 不删除user_confirmation标签，保持原内容显示
      }
    } catch (error) {
      console.error('解析用户确认JSON失败:', error, '原始内容:', userConfirmationMatch[1])
    }
  } else {
    console.log('未匹配到user_confirmation标签')
  }
  
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
    
    // 使用 Markdown 折叠块显示工具调用
    return `
<details>
<summary>${toolName}</summary>

\`\`\`
${toolContent.trim()}
\`\`\`

</details>
`
  })
  
  // 处理普通工具调用标签 <tool_name>content</tool_name>（保留原有功能）
  // 修改正则表达式以支持带点号的标签名，如 <default_api.command_executor>
  // 使用更健壮的匹配策略来处理包含 < 符号的内容
  
  // 先找到所有可能的开始标签，限制标签名长度不超过25个字符
  const toolTagPattern = /<([a-zA-Z_][a-zA-Z0-9_.]{0,24})>/g
  const foundTags = []
  let tagMatch
  
  // 收集所有开始标签的位置信息
  while ((tagMatch = toolTagPattern.exec(processed)) !== null) {
    const tagName = tagMatch[1]
    const startPos = tagMatch.index
    const startTagEnd = tagMatch.index + tagMatch[0].length
    
    // 额外检查：确保标签名不超过25个字符
    if (tagName.length > 25) {
      continue
    }
    
    // 寻找对应的结束标签
    const endTagPattern = new RegExp(`<\\/${tagName}>`, 'g')
    endTagPattern.lastIndex = startTagEnd // 从开始标签后开始搜索
    
    const endMatch = endTagPattern.exec(processed)
    if (endMatch) {
      const endPos = endMatch.index
      const endTagEnd = endMatch.index + endMatch[0].length
      const content = processed.substring(startTagEnd, endPos)
      
      // 跳过引用类型的标签（有link和index属性的）
      if (content.includes('link=') && content.includes('index=')) {
        continue
      }
      
      // 额外验证：确保这是一个合理的工具标签
      // 工具标签内容通常包含JSON或其他结构化内容
      const isLikelyToolTag = content.trim().length > 0 && (
        content.includes('{') || 
        content.includes('```') || 
        content.includes('action') ||
        content.includes('path') ||
        content.includes('content')
      )
      
      if (!isLikelyToolTag) {
        continue
      }
      
      foundTags.push({
        tagName,
        startPos,
        startTagEnd,
        endPos,
        endTagEnd,
        content,
        fullMatch: processed.substring(startPos, endTagEnd)
      })
    }
  }
  
  // 按位置从后往前排序，并记录需要的偏移量调整
  foundTags.sort((a, b) => b.startPos - a.startPos)
  
  // 为避免位置偏移问题，我们构建替换映射，然后一次性进行替换
  const replacements: Array<{
    startPos: number
    endPos: number 
    replacement: string
    original: string
  }> = []
  
  foundTags.forEach(tag => {
    console.log('检测到工具调用标签:', {
      toolName: tag.tagName,
      contentLength: tag.content.length,
      startPos: tag.startPos
    })
    
    const toolId = `tool-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    toolCalls.push({
      id: toolId,
      name: tag.tagName,
      content: tag.content.trim(),
      position: tag.startPos
    })
    
    // 使用 Markdown 折叠块来显示工具调用内容
    const replacement = `
<details>
<summary>${tag.tagName}</summary>

${tag.content.trim()}

</details>
`
    
    replacements.push({
      startPos: tag.startPos,
      endPos: tag.endTagEnd,
      replacement: replacement,
      original: tag.fullMatch
    })
  })
  
  // 执行替换，从后往前以避免位置偏移
  replacements.forEach(rep => {
    processed = processed.substring(0, rep.startPos) + rep.replacement + processed.substring(rep.endPos)
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
    
    // 使用 Markdown 折叠块显示引用内容
    return `
<details>
<summary>📚 ${tagName}_${index}</summary>

**链接**: [${decodeURIComponent(link)}](${decodeURIComponent(link)})

**引用文本**:
${text}

</details>
`
  })
  
  // 处理旧的ref标签
  processed = processed.replace(/<ref>\[(.*?)\]<\/ref>/g, '[$1]')
  // 移除answer标签但保留内容
  processed = processed.replace(/<answer>([\s\S]*?)<\/answer>/g, '$1')
  
  // 后处理：在连续的标签容器间添加换行控制
  // 修复的方法：使用更宽松的正则表达式来匹配包含换行符的标签
  const tagMatches = processed.match(/<span[^>]*class="tag-container"[^>]*>[\s\S]*?<\/span>/g)
  console.log('标签换行处理调试信息:', {
    tagMatches: tagMatches?.map((match, index) => ({
      index,
      preview: match.substring(0, 100).replace(/\s+/g, ' ') + '...',
      length: match.length
    })),
    tagCount: tagMatches ? tagMatches.length : 0,
    shouldAddLineBreaks: tagMatches && tagMatches.length > 3,
    timestamp: new Date().toLocaleTimeString()
  })
  
  if (tagMatches && tagMatches.length > 3) {
    let tagIndex = 0
    processed = processed.replace(
      /<span[^>]*class="tag-container"[^>]*>[\s\S]*?<\/span>/g,
      (tagMatch) => {
        tagIndex++
        const shouldAddBreak = tagIndex > 3 && (tagIndex - 1) % 3 === 0
        // console.log(`处理第${tagIndex}个标签:`, {
        //   shouldAddBreak,
        //   tagIndex,
        //   calculation: (tagIndex - 1) % 3,
        //   tagPreview: tagMatch.substring(0, 80).replace(/\s+/g, ' ') + '...'
        // })
        // 在第4、7、10...个标签前添加换行
        if (shouldAddBreak) {
          return `<br/>${tagMatch}`
        }
        return tagMatch
      }
    )
    
    // console.log('换行处理完成:', {
    //   originalLength: originalProcessed.length,
    //   newLength: processed.length,
    //   addedBreaks: processed.split('<br/>').length - 1,
    //   hasChanges: originalProcessed !== processed
    // })
  }
  
  console.log('内容处理完成:', {
    originalLength: text.length,
    processedLength: processed.length,
    toolCallsCount: toolCalls.length,
    referencesCount: references.length,
    hasUserConfirmation: !!userConfirmation,
    timestamp: new Date().toLocaleTimeString()
  })
  
  return {
    processedText: processed,
    toolCalls,
    references,
    userConfirmation
  }
}

// 计算处理后的内容 - 增加强制更新机制
const processedContent = computed(() => {
  // 添加一个依赖追踪，确保内容变化时重新计算
  const content = props.content
  const isStreaming = props.isStreaming
  const updateKey = forceUpdateKey.value // 使用强制更新键
  
  const result = processContent(content)
  
  // 如果是流式输出，强制每次都重新处理
  if (isStreaming && !props.isUser) {
    console.log('processedContent 计算 (流式):', {
      contentLength: content?.length,
      toolCallsCount: result.toolCalls.length,
      referencesCount: result.references.length,
      updateKey,
      timestamp: new Date().toLocaleTimeString()
    })
  }
  
  return result
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
      'chat-theme',
      props.isUser ? 'user-message message-bubble-user' : 'ai-message message-bubble-ai',
      { 'streaming-message': props.isStreaming, 'error-message': props.isError }
    ]"
    shadow="never"
    body-style="padding:12px 16px; display: block; max-width: 100%; word-wrap: break-word;"
    :style="{ minHeight: bubbleMinHeight }"
  >
    <!-- 用户消息 -->
    <template v-if="props.isUser">
      <!-- 使用新的文件列表组件 -->
      <MessageFileList 
        :files="displayFiles"
        @preview-file="handleFilePreview"
        @download-file="handleFileDownload"
      />
      <!-- 用户消息内容 -->
      <div class="user-message-content">
        {{ props.content }}
      </div>
    </template>

    <!-- AI消息 -->
    <template v-else>
      <!-- 主要内容 -->
      <div 
        class="main-content message-content" 
        :data-streaming="props.isStreaming"
        @click="handleTagClick"
      >
        <!-- 对于正在流式输出的内容，也进行markdown渲染 -->
        <template v-if="props.isStreaming">
          <!-- 流式输出时也进行完整的markdown渲染 -->
          <div class="md-content streaming-content" v-html="md.render(processedContent.processedText)" />
        </template>
        <template v-else>
          <!-- 流式完成后渲染markdown格式 -->
          <div class="md-content" v-html="md.render(processedContent.processedText)" />
        </template>
        
        <!-- 流式输出指示器 -->
        <div v-if="props.isStreaming && !props.isPaused && !props.content.includes('🤔') && !props.content.includes('⏳') && !props.content.includes('❌')" class="streaming-indicator">
          <span class="cursor">|</span>
        </div>
        
        <!-- 暂停状态指示器 -->
        <div v-if="props.isPaused && !props.isUser" class="pause-indicator">
          <div class="pause-content">
            <div class="pause-spinner">
              <div class="spinner-ring"></div>
            </div>
            <span class="pause-text">暂停中，正在等待用户指示</span>
          </div>
        </div>
      </div>
      
      <!-- 用户确认组件 - 只在流式输出且检测到user_confirmation时显示 -->
      <LLMUserConfirmation
        v-if="props.isStreaming && processedContent.userConfirmation && props.uid && props.sid"
        :message="processedContent.userConfirmation.message"
        :uid="props.uid"
        :sid="props.sid"
        :visible="true"
        @send-confirmation="handleUserConfirmation"
      />
    </template>
  </el-card>
</template>

<style scoped>
.user-message {
  background: var(--chat-user-message-bg);
  color: var(--chat-user-message-text);
  border: 1px solid var(--chat-primary);
  border-radius: 12px;
  transition: min-height 0.3s ease-out;
  width: 100%;
  word-wrap: break-word;
  box-sizing: border-box;
}

/* 用户消息内容 */
.user-message-content {
  margin-top: 0;
}

.ai-message {
  background: var(--chat-ai-message-bg);
  color: var(--chat-ai-message-text);
  border: 1px solid var(--chat-border);
  border-radius: 12px;
  transition: min-height 0.3s ease-out;
  width: 100%;
  word-wrap: break-word;
  box-sizing: border-box;
}

.streaming-message {
  border-color: var(--chat-primary) !important;
  box-shadow: 0 0 0 1px var(--chat-primary-light) !important;
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
  border-color: var(--chat-error) !important;
  background: rgba(239, 68, 68, 0.1) !important;
  color: var(--chat-error) !important;
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

/* 暂停状态指示器样式 */
.pause-indicator {
  margin: 12px 0;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 12px;
  border-left: 4px solid #f59e0b;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.pause-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pause-spinner {
  position: relative;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.spinner-ring {
  width: 20px;
  height: 20px;
  border: 2px solid transparent;
  border-top: 2px solid #f59e0b;
  border-right: 2px solid #f59e0b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.pause-text {
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.025em;
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

/* 流式输出内容现在使用 md-content 类，无需额外样式 */

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
  background-color: rgba(102, 8, 163, 0.15);
  padding: 2px 4px;
  border-radius: 3px;
  word-break: break-all;
  white-space: pre-wrap;
  border: 1px solid rgba(102, 8, 163, 0.2);
}

/* 夜间模式下的code样式 */
[data-theme="dark"] .md-content :deep(code) {
  background-color: rgba(102, 8, 163, 0.25);
  color: #ffffff;
  border-color: rgba(102, 8, 163, 0.4);
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

/* Markdown引用块样式 */
.md-content :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 16px;
  border-left: 4px solid #6608a3;
  background-color: rgba(102, 8, 163, 0.1);
  border-radius: 6px;
  color: #333;
  font-style: italic;
}

/* 夜间模式下的引用块样式 */
[data-theme="dark"] .md-content :deep(blockquote) {
  background-color: rgba(20, 20, 20, 0.9);
  color: #ffffff;
  border-left-color: rgba(102, 8, 163, 0.8);
  border: 1px solid rgba(102, 8, 163, 0.3);
}

/* 夜间模式下的暂停指示器样式 */
[data-theme="dark"] .pause-indicator {
  background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
  border-left-color: #fbbf24;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

[data-theme="dark"] .pause-text {
  color: #d1d5db;
}

[data-theme="dark"] .spinner-ring {
  border-top-color: #fbbf24;
  border-right-color: #fbbf24;
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
  /* 移动端消息气泡整体优化 */
  .user-message {
    padding: 4px 8px; /* 大幅减少内边距 */
    font-size: 0.7rem; /* 显著减小字体 */
    line-height: 1.2; /* 更紧凑的行高 */
    border-radius: 10px; /* 减小圆角 */
    max-width: 100%;
    margin: 2px 0; /* 减少消息间距 */
  }
  
  .user-message-content {
    font-size: 0.7rem;
    line-height: 1.2;
  }
  
  .ai-message {
    padding: 4px 8px; /* 大幅减少内边距 */
    font-size: 0.7rem; /* 显著减小字体 */
    line-height: 1.2; /* 更紧凑的行高 */
    border-radius: 10px; /* 减小圆角 */
    max-width: 100%;
    margin: 2px 0; /* 减少消息间距 */
  }
  
  /* 移动端主要内容区域 */
  .main-content {
    font-size: 0.7rem;
    line-height: 1.2;
    margin: 0;
    padding: 0;
  }
  
  /* 移动端markdown内容核心优化 */
  .md-content {
    font-size: 0.7rem !important; /* 核心字体大小 */
    line-height: 1.2 !important; /* 紧凑行高 */
  }
  
  /* 移动端段落优化 */
  .md-content :deep(p) {
    margin: 1px 0 2px 0; /* 极小的段落间距 */
    font-size: 0.7rem;
    line-height: 1.2;
  }
  
  /* 移动端标题优化 */
  .md-content :deep(h1), 
  .md-content :deep(h2), 
  .md-content :deep(h3),
  .md-content :deep(h4),
  .md-content :deep(h5),
  .md-content :deep(h6) {
    font-size: 0.75rem; /* 统一较小的标题字体 */
    margin: 2px 0; /* 大幅减少标题间距 */
    font-weight: 600;
  }
  
  /* 移动端列表优化 */
  .md-content :deep(ul), 
  .md-content :deep(ol) {
    margin: 2px 0; /* 减少列表间距 */
    padding-left: 12px; /* 减少缩进 */
    font-size: 0.7rem;
  }
  
  .md-content :deep(li) {
    margin: 1px 0; /* 极小的列表项间距 */
    line-height: 1.2;
  }
  
  /* 移动端代码块优化 */
  .md-content :deep(pre) {
    padding: 4px 6px; /* 减少代码块内边距 */
    margin: 3px 0; /* 减少代码块间距 */
    font-size: 0.65rem; /* 更小的代码字体 */
    border-radius: 4px;
    overflow-x: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
  
  /* 移动端行内代码优化 */
  .md-content :deep(code) {
    font-size: 0.65rem; /* 更小的内联代码字体 */
    padding: 1px 2px; /* 减少内联代码内边距 */
    border-radius: 2px;
  }
  
  /* 移动端引用块优化 */
  .md-content :deep(blockquote) {
    margin: 3px 0;
    padding: 3px 6px;
    border-left: 2px solid #ddd;
    font-size: 0.7rem;
    background: rgba(0, 0, 0, 0.02);
    border-radius: 3px;
  }
  
  /* 移动端数学公式优化 */
  .md-content :deep(.katex) {
    font-size: 0.65rem !important;
  }
  
  .md-content :deep(.katex-display) {
    margin: 3px 0;
    padding: 3px;
    font-size: 0.6rem !important;
  }
  
  .md-content :deep(.katex-display > .katex) {
    font-size: 0.65rem !important;
  }
  
  /* 移动端表格优化 */
  .md-content :deep(table) {
    font-size: 0.6rem; /* 更小的表格字体 */
    width: 100%;
    overflow-x: auto;
    display: block;
    white-space: nowrap;
  }
  
  .md-content :deep(th), 
  .md-content :deep(td) {
    padding: 2px 4px; /* 减少表格单元格内边距 */
    font-size: 0.6rem;
  }
  
  /* 移动端工具调用折叠块优化 */
  .md-content :deep(details) {
    margin: 3px 0;
    font-size: 0.65rem;
    border-radius: 4px;
  }
  
  .md-content :deep(details > summary) {
    padding: 3px 6px;
    font-size: 0.65rem;
    border-radius: 3px;
  }
  
  .md-content :deep(details pre) {
    font-size: 0.6rem;
    padding: 3px 4px;
    margin: 1px 0;
  }
  
  /* 移动端标签样式优化 */
  :deep(.inline-tag) {
    padding: 1px 3px;
    margin: 0 1px;
    font-size: 0.6rem;
    border-radius: 2px;
    line-height: 1.1;
    display: inline-block;
    vertical-align: baseline;
  }
  
  /* 移动端展开内容优化 */
  :deep(.tag-expanded-content) {
    padding: 3px 4px;
    margin: 1px 0;
    font-size: 0.65rem;
    border-radius: 3px;
    max-height: 80px; /* 限制移动端展开内容高度 */
    overflow-y: auto;
  }
  
  /* 移动端流式输出指示器优化 */
  .streaming-indicator {
    font-size: 0.6rem;
  }
  
  .cursor {
    font-size: 0.6rem;
    line-height: 1.1;
  }
}

/* 工具调用折叠块样式 */
.md-content :deep(details) {
  margin: 12px 0;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background-color: #fafafa;
}

.md-content :deep(details > summary) {
  padding: 12px 16px;
  background-color: #f5f5f5;
  border-bottom: 1px solid #e8e8e8;
  cursor: pointer;
  font-weight: 600;
  color: #333;
  user-select: none;
  transition: background-color 0.2s ease;
}

.md-content :deep(details > summary:hover) {
  background-color: #e9ecef;
}

.md-content :deep(details[open] > summary) {
  background-color: #e3f2fd;
  color: #1976d2;
  border-bottom-color: #bbdefb;
}

.md-content :deep(details > summary::marker) {
  color: #1976d2;
}

.md-content :deep(details > *:not(summary)) {
  padding: 16px;
  background-color: #ffffff;
  border-top: 1px solid #e8e8e8;
}

.md-content :deep(details pre) {
  margin: 0;
  background-color: #f8f9fa !important;
  border: 1px solid #e9ecef !important;
  border-radius: 4px !important;
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

  /* 深色主题下的折叠块样式 */
  .md-content :deep(details) {
    border-color: #444;
    background-color: #2a2a2a;
  }

  .md-content :deep(details > summary) {
    background-color: #333;
    border-bottom-color: #444;
    color: #fff;
  }

  .md-content :deep(details > summary:hover) {
    background-color: #404040;
  }

  .md-content :deep(details[open] > summary) {
    background-color: #1e3a8a;
    color: #93c5fd;
  }

  .md-content :deep(details > *:not(summary)) {
    background-color: #1a1a1a;
    border-top-color: #444;
  }

  .md-content :deep(details pre) {
    background-color: #2d3748 !important;
    border-color: #4a5568 !important;
  }
}

/* 标签容器样式 - 支持内联展开 */
:deep(.tag-container) {
  position: relative;
  display: inline-block;
  margin: 0 2px 2px 0; /* 右边距和下边距，为换行留出空间 */
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