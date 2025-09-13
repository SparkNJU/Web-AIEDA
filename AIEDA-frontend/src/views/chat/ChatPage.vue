<!-- ChatPage.vue -->
<script setup lang="ts">
import { ref, nextTick, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// 导入子组件
import ChatAside from './ChatAside.vue'
import MessageList from './MessageList.vue'
import ChatInput, { type AgentType, type InputType } from './ChatInput.vue'
import WelcomeCard from './WelcomeCard.vue'
import FilePreview from '../../components/File/FilePreview.vue'
// 导入API
import { createSession, getSessionRecords, getUserSessions, sendMessageStream, sendMessageInput, updateSessionTitle, deleteSession } from '../../api/chat'
import { type FileVO } from '../../api/file'

// 类型定义
export type SessionRecord = {
  sid: number
  title: string
  createTime: string
  updateTime: string
}

export type ChatRecord = {
  rid?: number
  sid: number
  direction: boolean // true=用户，false=AI
  content: string
  sequence?: number
  type?: number
  createTime?: string
  isStreaming?: boolean // 是否正在流式输出
  isError?: boolean // 是否为错误消息
  agentType?: string // Agent类型
  inputType?: string // 输入类型，config类型的消息不显示
  attachedFiles?: FileVO[] // 新增：附件文件列表（用于刚发送的消息）
  _updateTimestamp?: number // 内部使用：强制更新时间戳
  _completeTimestamp?: number // 内部使用：完成时间戳
  _errorTimestamp?: number // 内部使用：错误时间戳
  _connectionId?: number // 内部使用：连接标识
}

// 核心数据
const router = useRouter()
const userId = ref<number>(0) 
const currentSessionId = ref<number>(0)
const currentSessionTitle = ref('')
const sessions = ref<SessionRecord[]>([])

// 为每个会话独立管理状态
const sessionStates = ref<Record<number, {
  messages: ChatRecord[]
  inputMessage: string
  isStreaming: boolean
  currentStreamMessage: string
  streamingMessageIndex?: number
}>>({})

const isLoading = ref(false)
const tempInputMessage = ref('') // 临时输入消息，用于没有会话时的输入
let scrollTimer: number | null = null // 滚动防抖定时器

// 文件预览相关状态
const showFilePreview = ref(false)
const previewFileId = ref('')
const previewFile = ref<FileVO | null>(null)
const asideCollapsed = ref(false) // 侧边栏收起状态

// 拖拽调整宽度相关状态
const chatMainWidth = ref(50) // 对话窗口宽度百分比，默认50%
const isResizingMain = ref(false)
const directoryWidth = ref(250) // 文件目录宽度，默认250px
const isResizingDirectory = ref(false)

const suggestionQuestions = [
"AI 如何提升 EDA 全链路仿真性能？有实测吗？",
"系统级仿真各环节如何衔接？传递函数作用？",
];

// 计算属性
const currentSessionState = computed(() => {
  const sessionId = currentSessionId.value
  if (sessionId === 0) {
    return {
      messages: [],
      inputMessage: '',
      isStreaming: false,
      currentStreamMessage: '',
      streamingMessageIndex: undefined
    }
  }
  if (!sessionStates.value[sessionId]) {
    sessionStates.value[sessionId] = {
      messages: [],
      inputMessage: '',
      isStreaming: false,
      currentStreamMessage: '',
      streamingMessageIndex: undefined
    }
  }
  return sessionStates.value[sessionId]
})

const messages = computed(() => {
  // 过滤掉 inputType 为 config 的消息
  return currentSessionState.value.messages.filter(msg => msg.inputType !== 'config')
})
const inputMessage = computed({
  get: () => {
    // 如果没有当前会话，返回临时输入值
    if (currentSessionId.value === 0) {
      return tempInputMessage.value
    }
    return currentSessionState.value.inputMessage
  },
  set: (value: string) => {
    if (currentSessionId.value !== 0) {
      currentSessionState.value.inputMessage = value
    } else {
      // 没有会话时，保存到临时变量
      tempInputMessage.value = value
    }
  }
})
const isStreaming = computed(() => currentSessionState.value.isStreaming)

const showWelcomeCard = computed(() => {
  return currentSessionId.value === 0 || (currentSessionId.value !== 0 && messages.value.length === 0)
})

const inputDisabled = computed(() => {
  return isLoading.value || isStreaming.value || !userId.value
})

// 计算聊天布局的动态宽度
const chatLayoutWidth = computed(() => {
  if (!showFilePreview.value) return '100%'
  
  // 返回动态的宽度百分比，确保不会超出屏幕
  return `${Math.max(30, Math.min(70, chatMainWidth.value))}%`
})

// 检查当前会话是否有文件（简化实现）
const hasUploadedFiles = computed(() => {
  // 简化：如果有当前会话，可能就有文件
  return currentSessionId.value > 0
})

// 初始化加载
onMounted(() => {
  loadUserSessions()
})

// 组件卸载时清理
onUnmounted(() => {
  if (scrollTimer) {
    clearTimeout(scrollTimer)
    scrollTimer = null
  }
  
  // 清理拖拽相关的事件监听器
  document.removeEventListener('mousemove', handleResizeMain)
  document.removeEventListener('mouseup', stopResizeMain)
  document.removeEventListener('mousemove', handleResizeDirectory)
  document.removeEventListener('mouseup', stopResizeDirectory)
  document.body.style.cursor = 'auto'
  document.body.style.userSelect = 'auto'
})

// 加载用户会话列表
const loadUserSessions = async () => {
  try {
    userId.value = Number(sessionStorage.getItem('uid'));
    console.log('正在加载用户会话，用户ID:', userId.value)
    const res = await getUserSessions(userId.value)
    console.log('获取会话响应:', res.data)
    if (res.data.code === '200') {
      sessions.value = res.data.data.sort((a: SessionRecord, b: SessionRecord) => 
        b.updateTime.localeCompare(a.updateTime)
      )
      console.log('处理后的会话列表:', sessions.value)
    } else {
      ElMessage.error(res.data.message || '获取会话列表失败')
    }
  } catch (error) {
    console.error('获取会话列表失败:', error)
    ElMessage.error('获取会话列表失败')
  }
}

// 会话相关方法
const handleCreateSession = async () => {
  if (!userId.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  isLoading.value = true
  try {
    const res = await createSession(userId.value)
    if (res.data.code === '200') {
      const newSession = res.data.data
      sessions.value.unshift(newSession)
      currentSessionId.value = newSession.sid
      currentSessionTitle.value = newSession.title
      // 为新会话初始化状态
      sessionStates.value[newSession.sid] = {
        messages: [],
        inputMessage: '',
        isStreaming: false,
        currentStreamMessage: '',
        streamingMessageIndex: undefined
      }
    } else {
      ElMessage.error(res.data.message || '创建会话失败')
    }
  } catch (error) {
    console.error('创建会话失败:', error)
    ElMessage.error('创建会话失败')
  } finally {
    isLoading.value = false
  }
}

const handleSelectSession = (sessionId: number) => {
  if (currentSessionId.value === sessionId) return
  currentSessionId.value = sessionId
  
  // 检查目标会话是否正在流式回复中
  const targetSessionState = sessionStates.value[sessionId]
  if (targetSessionState && targetSessionState.isStreaming) {
    // 如果正在流式回复，不重新加载历史记录，直接切换到该会话
    console.log(`[会话切换] 会话${sessionId}正在流式回复中，跳过历史记录加载`)
    currentSessionTitle.value = sessions.value.find(s => s.sid === sessionId)?.title || ''
    // 滚动到底部以显示最新内容
    scrollToBottom()
  } else if (targetSessionState && targetSessionState.messages.length > 0) {
    // 如果会话状态已存在且有消息，也不重新加载历史记录
    console.log(`[会话切换] 会话${sessionId}已有消息缓存，跳过历史记录加载`)
    currentSessionTitle.value = sessions.value.find(s => s.sid === sessionId)?.title || ''
    scrollToBottom()
  } else {
    // 如果没有在流式回复且没有消息缓存，正常加载历史记录
    loadChatHistory(sessionId)
    currentSessionTitle.value = sessions.value.find(s => s.sid === sessionId)?.title || ''
  }
}

const loadChatHistory = async (sessionId: number) => {
  // 检查会话是否正在流式回复中
  if (sessionStates.value[sessionId] && sessionStates.value[sessionId].isStreaming) {
    console.log(`[历史记录加载] 会话${sessionId}正在流式回复中，跳过历史记录加载`)
    return
  }

  isLoading.value = true
  try {
    const res = await getSessionRecords(sessionId)
    if (res.data.code === '200') {
      // 确保会话状态存在
      if (!sessionStates.value[sessionId]) {
        sessionStates.value[sessionId] = {
          messages: [],
          inputMessage: '',
          isStreaming: false,
          currentStreamMessage: '',
          streamingMessageIndex: undefined
        }
      }
      
      // 再次检查是否在流式回复中（防止加载过程中状态发生变化）
      if (!sessionStates.value[sessionId].isStreaming) {
        sessionStates.value[sessionId].messages = res.data.data.map((msg: ChatRecord) => ({
          ...msg,
          content: msg.content
        }))
        scrollToBottom()
      } else {
        console.log(`[历史记录加载] 会话${sessionId}在加载过程中开始流式回复，取消历史记录更新`)
      }
    } else {
      ElMessage.error(res.data.message || '获取聊天历史失败')
    }
  } catch (error) {
    console.error('获取聊天历史失败:', error)
    ElMessage.error('获取聊天历史失败')
  } finally {
    isLoading.value = false
  }
}

const handleEditSessionTitle = async (sessionId: number, newTitle: string) => {
  try {
    const res = await updateSessionTitle(userId.value, sessionId, newTitle)
    if (res.data.code === '200') {
      const session = sessions.value.find(s => s.sid === sessionId)
      if (session) session.title = newTitle
      if (currentSessionId.value === sessionId) currentSessionTitle.value = newTitle
    } else {
      ElMessage.error(res.data.message || '更新会话标题失败')
    }
  } catch (error) {
    console.error('更新会话标题失败:', error)
    ElMessage.error('更新会话标题失败')
  }
}

const handleDeleteSession = async (sessionId: number) => {
  try {
    const res = await deleteSession(sessionId, userId.value)
    if (res.data.code === '200') {
      sessions.value = sessions.value.filter(s => s.sid !== sessionId)
      // 删除会话状态
      if (sessionStates.value[sessionId]) {
        delete sessionStates.value[sessionId]
      }
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = 0
        currentSessionTitle.value = ''
      }
    } else {
      ElMessage.error(res.data.message || '删除会话失败')
    }
  } catch (error) {
    console.error('删除会话失败:', error)
    ElMessage.error('删除会话失败')
  }
}

// 处理用户确认选择
const handleUserConfirmation = async (choice: '1' | '2') => {
  console.log('ChatPage: 接收到用户确认选择:', choice)
  
  if (!userId.value || !currentSessionId.value) {
    console.error('缺少用户ID或会话ID')
    return
  }
  
  // 使用LLMIntervention的软干预方法发送确认信息
  try {
    const requestData = {
      uid: userId.value,
      sid: currentSessionId.value,
      content: choice,
      inputType: "intervention" as "intervention",
      metadata: {
        type: "soft"
      }
    }

    console.log('发送用户确认到后端:', requestData)
    
    // 使用非流式接口发送确认信息
    const response = await sendMessageInput(requestData)

    if (response.code !== '200') {
      throw new Error(`用户确认请求失败: ${response.message}`)
    }

    console.log('用户确认发送成功')
    
  } catch (error) {
    console.error('发送用户确认失败:', error)
    ElMessage.error('发送确认失败，请重试')
  }
}

// 消息发送
const handleSendMessage = async (messageToSend: string, agentType: AgentType, inputType: InputType, files?: FileVO[]) => {
  console.log('接收到发送消息事件:', { messageToSend, agentType, inputType, files })
  
  // 如果没有当前会话，先创建一个新会话
  if (currentSessionId.value === 0) {
    await handleCreateSession()
    // 如果创建会话失败，直接返回
    if (currentSessionId.value === 0) {
      return
    }
    // 会话创建成功后，将临时输入消息转移到新会话
    if (tempInputMessage.value) {
      sessionStates.value[currentSessionId.value].inputMessage = tempInputMessage.value
      tempInputMessage.value = ''
    }
  }

  const sessionId = currentSessionId.value
  const sessionState = sessionStates.value[sessionId]
  
  // 根据输入类型决定使用流式还是非流式接口
  if (inputType === 'config' || inputType === 'intervention' || inputType === 'delete') {
    // 这些类型使用非流式接口，不需要建立SSE连接
    await handleSendMessageInput(messageToSend, files, agentType, inputType)
  } else {
    // question类型使用流式接口
    
    // 立即添加用户消息到界面，包含文件信息
    sessionState.messages.push({
      content: messageToSend,
      direction: true,
      sid: sessionId,
      attachedFiles: files && files.length > 0 ? [...files] : undefined // 复制文件数组，避免引用问题
    })
    sessionState.inputMessage = ''
    scrollToBottom()
    updateSessionTime()

    // 使用流式输出发送消息
    await handleSendMessageStream(messageToSend, files, agentType, inputType)
  }
}

// 非流式消息发送（用于config、delete、intervention类型）
const handleSendMessageInput = async (messageToSend: string, files?: FileVO[], agentType: AgentType = 'orchestrator', inputType: InputType = 'question') => {
  const sessionId = currentSessionId.value
  
  try {
    console.log('发送非流式消息:', { messageToSend, agentType, inputType, sessionId })
    
    // 解析配置信息（如果是config类型的消息）
    let configData = null
    let actualMessage = messageToSend
    let metadata: Record<string, any> = {
      agent_type: agentType
    }
    
    if (inputType === 'config' && messageToSend) {
      try {
        configData = JSON.parse(messageToSend)
        actualMessage = "" // config类型的消息内容为空
        // 将配置信息添加到metadata中
        metadata.llm_config = {
          api_key: configData.apiKey,
          base_url: configData.baseUrl,
          model: configData.model
        }
      } catch (e) {
        console.warn('无法解析config消息，使用默认配置:', e)
        actualMessage = ""
      }
    }
    
    // 准备请求数据
    const requestData = {
      uid: userId.value,
      sid: sessionId,
      content: actualMessage,
      inputType: inputType,
      metadata: metadata,
      fileReferences: files?.map(f => f.fileId)
    }
    
    // 发送非流式请求
    const response = await sendMessageInput(requestData)
    
    if (response.code !== '200') {
      throw new Error(response.message || '发送失败')
    }
    
    console.log('非流式消息发送成功:', inputType)
    
    // 对于config类型，显示成功提示
    if (inputType === 'config') {
      ElMessage.success('LLM配置已应用')
    }
    
  } catch (error) {
    console.error('非流式消息发送失败:', error)
    ElMessage.error(`发送${inputType}消息失败: ${error instanceof Error ? error.message : '未知错误'}`)
  }
}

// 流式消息发送
const handleSendMessageStream = async (messageToSend: string, files?: FileVO[], agentType: AgentType = 'orchestrator', inputType: InputType = 'question') => {
  const sessionId = currentSessionId.value
  const sessionState = sessionStates.value[sessionId]
  
  // 🔥 关键修复：重置流式状态，确保新的连接从干净状态开始
  sessionState.isStreaming = true
  sessionState.currentStreamMessage = '' // 清空累积消息
  sessionState.streamingMessageIndex = undefined // 重置流式消息索引
  
  console.log(`[连接管理] 🔄 开始新的流式连接 - sessionId: ${sessionId}, inputType: ${inputType}`)
  
  // 如果不是config类型的请求，才添加AI消息占位符
  let aiMessageIndex = -1
  if (inputType !== 'config') {
    // 添加AI消息占位符
    aiMessageIndex = sessionState.messages.length
    sessionState.streamingMessageIndex = aiMessageIndex
    sessionState.messages.push({
      content: '⏳ 连接中...',
      direction: false,
      sid: sessionId,
      isStreaming: true,
      // 添加唯一标识，确保每次都是新的消息对象
      _connectionId: Date.now()
    })
    scrollToBottom()
  }

  try {
    // 解析配置信息（如果是config类型的消息）
    let configData = null
    let actualMessage = messageToSend
    
    if (inputType === 'config' && messageToSend) {
      try {
        const configRequest = JSON.parse(messageToSend)
        configData = configRequest
        actualMessage = "" // config类型的消息内容为空
      } catch (e) {
        console.warn('无法解析config消息:', e)
      }
    }

    // 构造请求数据
    const requestData: any = {
      uid: userId.value,
      sid: sessionId,
      content: actualMessage,
      fileReferences: files && files.length > 0 ? files.map(f => f.fileId) : undefined,
      inputType: inputType, // 传递输入类型
      metadata: {
        agent_type: agentType // 将agentType放在metadata中
      }
    }
    
    // 如果是config类型且有配置数据，添加配置字段到metadata
    if (inputType === 'config' && configData) {
      requestData.metadata.api_key = configData.apiKey
      requestData.metadata.base_url = configData.baseUrl
      requestData.metadata.model = configData.model
    }

    // 使用统一的消息发送接口
    const response = await sendMessageStream(requestData)

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    // 读取SSE流
    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法获取响应流')
    }

    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let eventCount = 0
    
    console.log('[SSE连接] 开始读取流数据')
    
    // 处理单个SSE事件字符串
    const processSingleSSEEvent = async (eventStr: string, messageIndex: number) => {
      const lines = eventStr.split('\n')
      let eventType = ''
      let eventData = ''
      
      for (const line of lines) {
        const trimmedLine = line.trim()
        if (trimmedLine.startsWith('event:')) {
          eventType = trimmedLine.substring(6).trim()
        } else if (trimmedLine.startsWith('data:')) {
          eventData = trimmedLine.substring(5).trim()
        }
      }
      
      if (eventType && eventData) {
        console.log(`[SSE事件] 类型: ${eventType}, 数据长度: ${eventData.length}`)
        console.log(`[SSE事件] 原始数据: ${eventData.substring(0, 100)}${eventData.length > 100 ? '...' : ''}`)
        
        if (!eventData || eventData === '[DONE]' || eventData === '{}') {
          console.log('[SSE事件] 跳过空事件或结束标记')
          return
        }
        
        try {
          const parsedData = JSON.parse(eventData)
          console.log(`[SSE事件] 解析成功:`, parsedData)
          
          if (parsedData && parsedData.type) {
            await handleSSEEvent(parsedData, messageIndex, sessionId)
          } else {
            console.warn('[SSE事件] 解析的数据缺少type字段:', parsedData)
          }
        } catch (e) {
          console.warn('[SSE事件] 解析JSON失败:', eventData, e)
        }
      }
    }
    
    const readStream = async (): Promise<void> => {
      try {
        const { done, value } = await reader.read()
        
        if (done) {
          console.log(`[SSE连接] 流结束，共处理 ${eventCount} 个事件`)
          // 检查会话状态是否还存在，避免切换会话后报错
          if (sessionStates.value[sessionId]) {
            sessionStates.value[sessionId].isStreaming = false
            // 处理最后的完成状态，仅在有有效消息索引时
            if (aiMessageIndex !== -1 && sessionState.messages[aiMessageIndex]) {
              const msg = sessionState.messages[aiMessageIndex]
              sessionState.messages.splice(aiMessageIndex, 1, {
                ...msg,
                isStreaming: false
              })
            }
          }
          return
        }
        
        // 添加新数据到缓冲区
        const chunk = decoder.decode(value, { stream: true })
        buffer += chunk
        console.log(`[SSE连接] 接收数据块，长度: ${chunk.length}, 缓冲区总长度: ${buffer.length}`)
        
        // 按双换行符分割事件（SSE标准格式）
        const events = buffer.split('\n\n')
        
        // 保留最后一个可能不完整的事件
        if (events.length > 1) {
          buffer = events.pop() || ''
          
          // 处理完整的事件
          for (const eventStr of events) {
            if (eventStr.trim()) {
              await processSingleSSEEvent(eventStr.trim(), aiMessageIndex)
              eventCount++
            }
          }
        }
      } catch (error) {
        console.error('[SSE连接] 读取流数据时出错:', error)
        throw error
      }
      
      return readStream()
    }
    
    await readStream()
    
  } catch (error) {
    console.error('SSE连接失败:', error)
    ElMessage.error('连接失败，请检查网络或稍后重试')
    // 检查会话状态是否还存在
    if (sessionStates.value[sessionId]) {
      sessionStates.value[sessionId].isStreaming = false
      // 移除失败的AI消息，仅在有有效消息索引时
      if (aiMessageIndex !== -1 && sessionState.messages[aiMessageIndex]) {
        sessionState.messages.splice(aiMessageIndex, 1)
      }
    }
  }
}

// 处理SSE事件
const handleSSEEvent = async (eventData: any, messageIndex: number, sessionId: number) => {
  console.log(`[事件处理] 类型: ${eventData.type}, 内容长度: ${eventData.content?.length || 0}`)
  
  // 检查会话状态是否还存在，避免切换会话后报错
  if (!sessionStates.value[sessionId]) {
    console.log('[事件处理] 会话状态不存在，跳过处理')
    return
  }
  
  // 如果messageIndex为-1，说明是config类型的请求，不需要显示消息
  if (messageIndex === -1) {
    console.log('[事件处理] config类型请求，跳过消息显示')
    return
  }
  
  const sessionState = sessionStates.value[sessionId]
  
  switch (eventData.type) {
    case 'start':
      console.log('[START事件] AI开始思考:', eventData.message)
      // 重置累积内容为空字符串，不包含思考提示
      sessionState.currentStreamMessage = ''
      if (sessionState.messages[messageIndex]) {
        // 显示思考提示（仅用于UI展示），但不累加到最终内容
        const msg = sessionState.messages[messageIndex]
        sessionState.messages.splice(messageIndex, 1, { 
          ...msg, 
          content: eventData.message || 'AI正在思考...', 
          isStreaming: true 
        })
        await nextTick()
      }
      // 只有当前会话才滚动到底部
      if (currentSessionId.value === sessionId) {
        scrollToBottom()
      }
      break
      
    case 'delta':
    case 'message': // 兼容后端返回的message事件类型
      const deltaContent = eventData.content || ''
      console.log(`[DELTA事件] 片段长度: ${deltaContent.length}`)
      console.log(`[DELTA事件] 片段内容: ${deltaContent.substring(0, 50)}${deltaContent.length > 50 ? '...' : ''}`)
      console.log(`[DELTA事件] 当前累积长度: ${sessionState.currentStreamMessage.length}`)
      
      if (deltaContent) {
        // 累加内容到最终回复（不包含任何思考提示）
        const oldLength = sessionState.currentStreamMessage.length
        sessionState.currentStreamMessage += deltaContent
        console.log(`[DELTA事件] 累积后长度: ${sessionState.currentStreamMessage.length} (新增: ${sessionState.currentStreamMessage.length - oldLength})`)
        
        // 实时更新消息内容 - 强制触发Vue响应式更新
        if (sessionState.messages[messageIndex]) {
          const msg = sessionState.messages[messageIndex]
          // 创建新的消息对象，确保引用发生变化以触发重新渲染
          const updatedMsg = { 
            ...msg, 
            content: sessionState.currentStreamMessage, 
            isStreaming: true,
            // 添加时间戳确保每次都是新对象，强制触发响应式更新
            _updateTimestamp: Date.now()
          }
          sessionState.messages.splice(messageIndex, 1, updatedMsg)
          console.log(`[DELTA事件] UI更新完成，显示长度: ${sessionState.currentStreamMessage.length}`)
          
          // 强制触发Vue的重新渲染
          await nextTick()
          
          // 额外触发一次DOM更新，确保MessageBubble重新渲染
          await nextTick(() => {
            console.log('[DELTA事件] Vue DOM更新完成，MessageBubble应该已重新渲染markdown')
          })
        }
        
        // 优化滚动，使用防抖
        if (scrollTimer) {
          clearTimeout(scrollTimer)
        }
        scrollTimer = window.setTimeout(() => {
          // 只有当前会话才滚动到底部
          if (currentSessionId.value === sessionId) {
            scrollToBottom()
          }
          scrollTimer = null
        }, 50) // 增加防抖时间，避免过于频繁的滚动
      }
      break
      
    case 'complete':
      console.log('[COMPLETE事件] 回复完成:', eventData.message, 'recordId:', eventData.recordId)
      console.log(`[COMPLETE事件] 最终内容长度: ${sessionState.currentStreamMessage.length}`)
      sessionState.isStreaming = false
      
      // 清理防抖定时器
      if (scrollTimer) {
        clearTimeout(scrollTimer)
        scrollTimer = null
      }
      
      if (sessionState.messages[messageIndex]) {
        // 获取完整的回复内容
        const completeContent = sessionState.currentStreamMessage
        
        console.log('[COMPLETE事件] 设置最终消息内容')
        console.log(`[COMPLETE事件] 完整内容长度: ${completeContent.length}`)
        
        // 强制更新消息对象，确保触发响应式更新
        const finalMsg = {
          ...sessionState.messages[messageIndex],
          content: completeContent,
          isStreaming: false,
          // 添加完成时间戳，确保是新对象
          _completeTimestamp: Date.now()
        }
        if (eventData.recordId) {
          finalMsg.rid = eventData.recordId
        }
        
        // 使用splice确保触发响应式更新
        sessionState.messages.splice(messageIndex, 1, finalMsg)
        
        // 强制触发Vue的重新渲染
        await nextTick()
        console.log('[COMPLETE事件] Vue DOM更新完成，MessageBubble应该已重新渲染markdown')
        
        // 只有当前会话才滚动到底部
        if (currentSessionId.value === sessionId) {
          scrollToBottom()
        }
      }
      sessionState.currentStreamMessage = '' // 重置累积内容
      break
      
    case 'error':
      console.error('[ERROR事件] AI回复错误:', eventData.message || eventData.error)
      const errorMsg = eventData.message || eventData.error || '未知错误'
      ElMessage.error(`AI回复出错: ${errorMsg}`)
      sessionState.isStreaming = false
      if (sessionState.messages[messageIndex]) {
        // 强制更新错误消息对象
        const errorMsgObj = {
          ...sessionState.messages[messageIndex],
          content: `❌ 错误: ${errorMsg}`,
          isError: true,
          isStreaming: false,
          _errorTimestamp: Date.now()
        }
        sessionState.messages.splice(messageIndex, 1, errorMsgObj)
        // 强制触发Vue更新
        await nextTick()
      }
      sessionState.currentStreamMessage = '' // 重置累积内容
      break
      
    default:
      console.log('[未知事件] 类型:', eventData.type, '数据:', eventData)
  }
}

// 文件预览相关方法
const openFilePreview = (file: FileVO) => {
  previewFile.value = file
  previewFileId.value = file.fileId
  showFilePreview.value = true
  asideCollapsed.value = true // 收起侧边栏
}

const openFileBrowser = () => {
  // 打开文件浏览器，不指定特定文件
  previewFile.value = null
  previewFileId.value = ''
  showFilePreview.value = true
  asideCollapsed.value = true // 收起侧边栏
}

const toggleFilePreview = () => {
  // 切换文件预览窗口的显示/隐藏状态
  if (showFilePreview.value) {
    closeFilePreview()
  } else {
    openFileBrowser()
  }
}

const closeFilePreview = () => {
  showFilePreview.value = false
  previewFileId.value = ''
  previewFile.value = null
  asideCollapsed.value = false // 展开侧边栏
}

// 主窗口拖拽调整宽度的方法
const startResizeMain = (e: MouseEvent) => {
  e.preventDefault()
  isResizingMain.value = true
  document.addEventListener('mousemove', handleResizeMain)
  document.addEventListener('mouseup', stopResizeMain)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

const handleResizeMain = (e: MouseEvent) => {
  if (!isResizingMain.value) return
  
  const windowWidth = window.innerWidth
  const newWidth = (e.clientX / windowWidth) * 100
  
  // 限制在30%-70%范围内（±20%）
  const minWidth = 30
  const maxWidth = 70
  
  if (newWidth >= minWidth && newWidth <= maxWidth) {
    chatMainWidth.value = newWidth
    console.log('调整聊天窗口宽度:', newWidth + '%')
  }
}

const stopResizeMain = () => {
  isResizingMain.value = false
  document.removeEventListener('mousemove', handleResizeMain)
  document.removeEventListener('mouseup', stopResizeMain)
  document.body.style.cursor = 'auto'
  document.body.style.userSelect = 'auto'
}

// 文件目录拖拽调整宽度的方法
const startResizeDirectory = (e: MouseEvent) => {
  e.preventDefault()
  isResizingDirectory.value = true
  document.addEventListener('mousemove', handleResizeDirectory)
  document.addEventListener('mouseup', stopResizeDirectory)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

const handleResizeDirectory = (e: MouseEvent) => {
  if (!isResizingDirectory.value) return
  
  const filePreviewElement = document.querySelector('.file-preview-overlay')
  if (!filePreviewElement) return
  
  const rect = filePreviewElement.getBoundingClientRect()
  const relativeX = e.clientX - rect.left
  
  // 在200px到300px范围内调整（约5%屏幕宽度范围）
  const minWidth = 200
  const maxWidthPx = 300
  
  if (relativeX >= minWidth && relativeX <= maxWidthPx) {
    directoryWidth.value = relativeX
    console.log('调整目录宽度:', relativeX + 'px')
  }
}

const stopResizeDirectory = () => {
  isResizingDirectory.value = false
  document.removeEventListener('mousemove', handleResizeDirectory)
  document.removeEventListener('mouseup', stopResizeDirectory)
  document.body.style.cursor = 'auto'
  document.body.style.userSelect = 'auto'
}

// 暴露给模板使用
defineExpose({
  openFilePreview
})

// 工具方法
const updateSessionTime = () => {
  const session = sessions.value.find(s => s.sid === currentSessionId.value)
  if (!session) return
  
  // 使用时间戳确保总是最新，然后转换为ISO字符串
  const now = new Date()
  const newTime = now.toISOString()
  // console.log(`[会话排序] 更新会话${currentSessionId.value}时间: ${session.updateTime} -> ${newTime}`)
  session.updateTime = newTime
  
  // 不使用字符串比较，而是使用时间戳比较
  sessions.value = [...sessions.value].sort((a, b) => {
    const timeA = new Date(a.updateTime).getTime()
    const timeB = new Date(b.updateTime).getTime()
    return timeB - timeA // 降序排列，最新的在前面
  })
  
  // console.log('[会话排序] 排序后会话列表:', sessions.value.map(s => ({sid: s.sid, title: s.title, updateTime: s.updateTime})))
  
  // 首次发送消息时更新标题
  const sessionState = sessionStates.value[currentSessionId.value]
  if (sessionState && sessionState.messages.length === 1) {
    const message = sessionState.messages[0].content
    const title = message.trim().length > 10 
      ? message.trim().substring(0, 10) + '...' 
      : message.trim()
    session.title = title
    currentSessionTitle.value = title
    handleEditSessionTitle(currentSessionId.value, title)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    const scrollbar = document.querySelector('.chat-messages .el-scrollbar__wrap')
    if (scrollbar) {
      // 使用平滑滚动
      scrollbar.scrollTo({
        top: scrollbar.scrollHeight,
        behavior: 'smooth'
      })
    }
  })
}
</script>

<template>
  <div class="chat-container chat-theme">
    <div class="chat-content">
      <!-- 主聊天区域 -->
      <div 
        class="chat-layout" 
        :class="{ 'with-preview': showFilePreview }"
        :style="{ width: chatLayoutWidth, maxWidth: chatLayoutWidth }"
      >
        <!-- 侧边栏 -->
        <ChatAside
          :sessions="sessions"
          :current-session-id="currentSessionId"
          :is-loading="isLoading"
          :force-collapsed="asideCollapsed"
          @create-session="handleCreateSession"
          @select-session="(sessionId: number) => handleSelectSession(sessionId)"
          @edit-session="(sessionId: number, newTitle: string) => handleEditSessionTitle(sessionId, newTitle)"
          @delete-session="(sessionId: number) => handleDeleteSession(sessionId)"
        />

        <!-- 主内容区 -->
        <div class="chat-main">
          <!-- 标题栏 -->
          <div class="chat-main-header" v-if="currentSessionId !== 0 && messages.length > 0">
            <div class="header-content">
              <h2>{{ currentSessionTitle || '新会话' }}</h2>
            </div>
          </div>

          <!-- 消息内容区域 -->
          <div class="chat-main-content">
            <!-- 消息列表/欢迎卡片 -->
            <MessageList 
              v-if="currentSessionId !== 0 && messages.length > 0"
              :messages="messages"
              :has-file-preview="showFilePreview"
              :uid="userId"
              :sid="currentSessionId"
              @open-file-preview="openFilePreview"
              @send-confirmation="handleUserConfirmation"
            />
            <WelcomeCard 
              v-else-if="showWelcomeCard"
              :suggestions="suggestionQuestions"
              :has-files="hasUploadedFiles"
              @insert-question="(q: string) => { inputMessage = q }"
            />
          </div>

          <!-- 输入区域 - 固定在底部 -->
          <ChatInput 
            :input-message="inputMessage"
            :is-loading="isLoading"
            :input-disabled="inputDisabled"
            :is-streaming="isStreaming"
            :uid="userId"
            :sid="currentSessionId"
            @update:input-message="(val: string) => inputMessage = val"
            @send-message="handleSendMessage"
            @open-file-preview="openFilePreview"
            @toggle-file-preview="toggleFilePreview"
            @create-session="handleCreateSession"
          />
        </div>
      </div>

      <!-- 主窗口和文件预览之间的拖拽分割线 -->
      <div 
        v-if="showFilePreview" 
        class="main-resize-handle"
        @mousedown="startResizeMain"
      />

      <!-- 文件预览面板 -->
      <FilePreview
        v-if="showFilePreview"
        :uid="userId"
        :sid="currentSessionId"
        :visible="showFilePreview"
        :selected-file-id="previewFileId"
        :directory-width="directoryWidth"
        @update:visible="showFilePreview = $event"
        @close="closeFilePreview"
        @start-resize-directory="startResizeDirectory"
      />
    </div>
  </div>
</template>

<style scoped>
.chat-container {
  height: 100%; /* 填满main-content的高度 */
  background: var(--chat-bg-primary);
  /* 为了避免被Header遮挡，给聊天容器添加上边距 */
  padding-top: 80px;
  box-sizing: border-box;
}

.chat-content {
  height: 100%; /* 填满chat-container的高度 */
  width: 100%;
  background: var(--chat-bg-secondary);
  box-shadow: 0 2px 8px var(--chat-shadow);
  overflow: hidden;
  display: flex;
  flex-direction: row; /* 改为横向布局 */
}

/* 移动端适配 */
@media (max-width: 768px) {
  .chat-container {
    padding-top: 60px; /* 移动端Header更紧凑 */
  }
  
  .chat-layout {
    position: relative; /* 为绝对定位的侧边栏提供参考 */
  }
  
  .chat-main {
    width: 100% !important; /* 移动端聊天主区域占满屏幕 */
    margin-left: 0 !important; /* 移除左边距 */
  }
  
  .chat-main-header {
    padding: 8px 12px; /* 减少header内边距 */
  }
  
  .chat-main-header h2 {
    font-size: 1rem; /* 减小标题字体 */
  }
  
  .chat-main-content {
    width: 100% !important; /* 移动端内容区域占满宽度 */
    margin: 0 !important; /* 移除居中边距 */
    padding: 0 4px; /* 添加小的侧边距 */
  }
  
  /* 移动端文件预览处理 */
  .chat-main.with-preview {
    width: 100% !important;
  }
  
  .chat-main.with-preview .chat-main-content {
    width: 100% !important;
    margin: 0 !important;
    padding: 0 2px; /* 有文件预览时更小的边距 */
  }
}

.chat-layout {
  display: flex;
  height: 100%;
  flex: none; /* 改为固定宽度，不自动伸缩 */
  min-height: 0;
  transition: width 0.3s ease;
  min-width: 300px; /* 设置最小宽度 */
  max-width: none; /* 移除最大宽度限制 */
  overflow: hidden; /* 防止内容溢出 */
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0; /* 防止内容溢出 */
  position: relative;
  transition: all 0.3s ease;
  overflow: hidden; /* 防止内容溢出 */
  background-color: var(--chat-bg-primary); /* 使用主题变量而不是硬编码 */
}

/* 夜间模式下的chat-main特殊处理 */
[data-theme="dark"] .chat-main {
  background-color: #0f0f0f;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-layout {
    flex-direction: column;
  }

  .chat-main.with-preview {
    width: 100%; /* 移动端恢复全宽 */
  }
}

.chat-main-header {
  padding: 12px 20px;
  border-bottom: 1px solid var(--chat-border);
  background: var(--chat-bg-card);
  flex-shrink: 0;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.chat-main-header h2 {
  margin: 0;
  color: var(--chat-primary);
  font-size: 1.2rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.chat-main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 允许内容收缩 */
  overflow: hidden;
  position: relative;
  /* 确保内容区域填满可用空间 */
  height: 100%;
  width: 80%; /* 默认为屏幕80%宽度 */
  margin: 0 auto; /* 居中显示 */
  box-sizing: border-box; /* 包含padding和border在宽度计算中 */
}

/* 当有文件预览时，主内容区域调整为100%宽度 */
.chat-main.with-preview .chat-main-content {
  width: 100%;
  margin: 0; /* 移除居中，占满可用空间 */
}

/* 确保欢迎卡片和消息列表完全填充容器 */
.chat-main-content > * {
  flex: 1;
  height: 100%;
}

/* 主窗口和文件预览之间的拖拽分割线 */
.main-resize-handle {
  width: 4px;
  background: transparent;
  cursor: col-resize;
  z-index: 1001;
  transition: background-color 0.2s ease;
  flex-shrink: 0;
  border-left: 1px solid #e4e7ed;
}

.main-resize-handle:hover {
  background-color: #409eff;
  border-left: 1px solid #409eff;
}

.main-resize-handle:active {
  background-color: #337ecc;
  border-left: 1px solid #337ecc;
}
</style>