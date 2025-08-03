<!-- ChatPage.vue -->
<script setup lang="ts">
import { ref, nextTick, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// 导入子组件
import ChatAside from './ChatAside.vue'
import MessageList from './MessageList.vue'
import ChatInput from './ChatInput.vue'
import WelcomeCard from './WelcomeCard.vue'
import FilePreview from '../../components/FilePreview.vue'
// 导入API
import { createSession, getSessionRecords, getUserSessions, sendMessageStream, updateSessionTitle, deleteSession } from '../../api/chat'
import { sendMessageWithFilesStream, type FileVO } from '../../api/file'

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

const suggestionQuestions = [
"AI 如何提升 EDA 全链路仿真性能？有实测吗？",
"系统级仿真各环节如何衔接？传递函数作用？",
"信号输入接口如何适配 FDTD 等仿真需求？",
"实测数据如何提升 DFT 仿真精度？"
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

const messages = computed(() => currentSessionState.value.messages)
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

// 消息发送
const handleSendMessage = async (messageToSend: string, files?: FileVO[]) => {
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
  
  // 添加用户消息到界面
  let displayMessage = messageToSend
  if (files && files.length > 0) {
    displayMessage += `\n\n📎 附件 (${files.length} 个文件):`
    files.forEach(file => {
      displayMessage += `\n• ${file.originalName}`
    })
  }
  
  sessionState.messages.push({
    content: displayMessage,
    direction: true,
    sid: sessionId
  })
  sessionState.inputMessage = ''
  scrollToBottom()
  updateSessionTime()

  // 使用流式输出发送消息（带文件或不带文件）
  await handleSendMessageStream(messageToSend, files)
}

// 流式消息发送
const handleSendMessageStream = async (messageToSend: string, files?: FileVO[]) => {
  const sessionId = currentSessionId.value
  const sessionState = sessionStates.value[sessionId]
  
  sessionState.isStreaming = true
  sessionState.currentStreamMessage = ''
  
  // 添加AI消息占位符
  const aiMessageIndex = sessionState.messages.length
  sessionState.streamingMessageIndex = aiMessageIndex
  sessionState.messages.push({
    content: '⏳ 连接中...',
    direction: false,
    sid: sessionId,
    isStreaming: true
  })
  scrollToBottom()

  try {
    let response: Response
    
    if (files && files.length > 0) {
      // 带文件的消息发送
      response = await sendMessageWithFilesStream({
        uid: userId.value,
        sid: sessionId,
        content: messageToSend,
        fileReferences: files.map(f => f.fileId)
      })
    } else {
      // 普通消息发送
      response = await sendMessageStream({
        uid: userId.value,
        sid: sessionId,
        content: messageToSend
      })
    }

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
            // 处理最后的完成状态
            if (sessionState.messages[aiMessageIndex]) {
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
      // 移除失败的AI消息
      if (sessionState.messages[aiMessageIndex]) {
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
        
        // 实时更新消息内容
        if (sessionState.messages[messageIndex]) {
          const msg = sessionState.messages[messageIndex]
          sessionState.messages.splice(messageIndex, 1, { 
            ...msg, 
            content: sessionState.currentStreamMessage, 
            isStreaming: true 
          })
          console.log(`[DELTA事件] UI更新完成，显示长度: ${sessionState.currentStreamMessage.length}`)
          await nextTick()
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
        
        // 直接更新消息内容和状态
        sessionState.messages[messageIndex].content = completeContent
        sessionState.messages[messageIndex].isStreaming = false
        if (eventData.recordId) {
          sessionState.messages[messageIndex].rid = eventData.recordId
        }
        
        // 使用nextTick确保DOM更新完成后再进行下一步操作
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
        sessionState.messages[messageIndex].content = `❌ 错误: ${errorMsg}`
        sessionState.messages[messageIndex].isError = true
        delete sessionState.messages[messageIndex].isStreaming
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
}

const closeFilePreview = () => {
  showFilePreview.value = false
  previewFileId.value = ''
  previewFile.value = null
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
  <div class="chat-container">
    <div class="chat-content">
      <div class="chat-layout">
        <!-- 侧边栏 -->
        <ChatAside
          :sessions="sessions"
          :current-session-id="currentSessionId"
          :is-loading="isLoading"
          @create-session="handleCreateSession"
          @select-session="(sessionId: number) => handleSelectSession(sessionId)"
          @edit-session="(sessionId: number, newTitle: string) => handleEditSessionTitle(sessionId, newTitle)"
          @delete-session="(sessionId: number) => handleDeleteSession(sessionId)"
        />

        <!-- 主内容区 -->
        <div class="chat-main" :class="{ 'with-preview': showFilePreview }">
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
            />
            <WelcomeCard 
              v-else-if="showWelcomeCard"
              :suggestions="suggestionQuestions"
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
          />
        </div>

        <!-- 文件预览面板 -->
        <div v-if="showFilePreview" class="file-preview-panel">
          <FilePreview
            :file-id="previewFileId"
            :file="previewFile || undefined"
            :visible="showFilePreview"
            @update:visible="showFilePreview = $event"
            @close="closeFilePreview"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-container {
  height: 80vh;
  background-color: rgba(102, 8, 116, 0.02);

}

.chat-content {
  height: calc(100vh - 80px);
  width: 100%;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-layout {
  display: flex;
  height: 100%;
  width: 100%;
  flex: 1;
  min-height: 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0; /* 防止内容溢出 */
  position: relative;
  transition: all 0.3s ease;
}

/* 当有文件预览时，聊天主区域变窄 */
.chat-main.with-preview {
  flex: 0 0 60%; /* 聊天区域占60%宽度 */
}

.file-preview-panel {
  flex: 0 0 40%; /* 文件预览区域占40%宽度 */
  border-left: 1px solid #e4e7ed;
  background-color: #fff;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-layout {
    flex-direction: column;
  }

  .chat-main.with-preview {
    flex: 0 0 50%; /* 移动端聊天区域占50%高度 */
  }

  .file-preview-panel {
    flex: 0 0 50%; /* 移动端文件预览区域占50%高度 */
    border-left: none;
    border-top: 1px solid #e4e7ed;
  }
}

.chat-main-header {
  padding: 12px 20px; /* 减少垂直padding */
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  justify-content: center;
  align-items: center;
}

.chat-main-header h2 {
  margin: 0;
  color: rgb(102, 8, 116);
  font-size: 1.2rem;
  font-weight: 500;
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
}

/* 确保欢迎卡片和消息列表完全填充容器 */
.chat-main-content > * {
  flex: 1;
  height: 100%;
}
</style>