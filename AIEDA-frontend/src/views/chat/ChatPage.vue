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
// 导入API
import { createSession, getSessionRecords, getUserSessions, sendMessageStream, updateSessionTitle, deleteSession } from '../../api/chat'

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
const messages = ref<ChatRecord[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const currentStreamMessage = ref('') // 当前流式消息内容
const isStreaming = ref(false) // 是否正在流式输出
let scrollTimer: number | null = null // 滚动防抖定时器

const suggestionQuestions = [
"AI 如何提升 EDA 全链路仿真性能？有实测吗？",
"系统级仿真各环节如何衔接？传递函数作用？",
"信号输入接口如何适配 FDTD 等仿真需求？",
"实测数据如何提升 DFT 仿真精度？"
];

// 计算属性
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
      messages.value = []
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
  loadChatHistory(sessionId)
  currentSessionTitle.value = sessions.value.find(s => s.sid === sessionId)?.title || ''
}

const loadChatHistory = async (sessionId: number) => {
  isLoading.value = true
  try {
    const res = await getSessionRecords(sessionId)
    if (res.data.code === '200') {
      messages.value = res.data.data.map((msg: ChatRecord) => ({
        ...msg,
        content: msg.content
      }))
      scrollToBottom()
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
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = 0
        currentSessionTitle.value = ''
        messages.value = []
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
const handleSendMessage = async (messageToSend: string) => {
  // 如果没有当前会话，先创建一个新会话
  if (currentSessionId.value === 0) {
    await handleCreateSession()
    // 如果创建会话失败，直接返回
    if (currentSessionId.value === 0) {
      return
    }
  }

  // 添加用户消息到界面
  messages.value.push({
    content: messageToSend,
    direction: true,
    sid: currentSessionId.value
  })
  inputMessage.value = ''
  scrollToBottom()
  updateSessionTime()

  // 使用流式输出发送消息
  await handleSendMessageStream(messageToSend)
}

// 流式消息发送
const handleSendMessageStream = async (messageToSend: string) => {
  isStreaming.value = true
  currentStreamMessage.value = ''
  
  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    content: '⏳ 连接中...',
    direction: false,
    sid: currentSessionId.value,
    isStreaming: true
  })
  scrollToBottom()

  try {
    // 使用 chat.ts 中的 API 发送POST请求启动流式回复
    const response = await sendMessageStream({
      uid: userId.value,
      sid: currentSessionId.value,
      content: messageToSend
    })

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
            await handleSSEEvent(parsedData, messageIndex)
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
          isStreaming.value = false
          // 处理最后的完成状态
          if (messages.value[aiMessageIndex]) {
            const msg = messages.value[aiMessageIndex]
            messages.value.splice(aiMessageIndex, 1, {
              ...msg,
              isStreaming: false
            })
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
    isStreaming.value = false
    // 移除失败的AI消息
    if (messages.value[aiMessageIndex]) {
      messages.value.splice(aiMessageIndex, 1)
    }
  }
}

// 处理SSE事件
const handleSSEEvent = async (eventData: any, messageIndex: number) => {
  console.log(`[事件处理] 类型: ${eventData.type}, 内容长度: ${eventData.content?.length || 0}`)
  
  switch (eventData.type) {
    case 'start':
      console.log('[START事件] AI开始思考:', eventData.message)
      // 重置累积内容为空字符串，不包含思考提示
      currentStreamMessage.value = ''
      if (messages.value[messageIndex]) {
        // 显示思考提示（仅用于UI展示），但不累加到最终内容
        const msg = messages.value[messageIndex]
        messages.value.splice(messageIndex, 1, { 
          ...msg, 
          content: eventData.message || 'AI正在思考...', 
          isStreaming: true 
        })
        await nextTick()
      }
      scrollToBottom()
      break
      
    case 'delta':
    case 'message': // 兼容后端返回的message事件类型
      const deltaContent = eventData.content || ''
      console.log(`[DELTA事件] 片段长度: ${deltaContent.length}`)
      console.log(`[DELTA事件] 片段内容: ${deltaContent.substring(0, 50)}${deltaContent.length > 50 ? '...' : ''}`)
      console.log(`[DELTA事件] 当前累积长度: ${currentStreamMessage.value.length}`)
      
      if (deltaContent) {
        // 累加内容到最终回复（不包含任何思考提示）
        const oldLength = currentStreamMessage.value.length
        currentStreamMessage.value += deltaContent
        console.log(`[DELTA事件] 累积后长度: ${currentStreamMessage.value.length} (新增: ${currentStreamMessage.value.length - oldLength})`)
        
        // 实时更新消息内容
        if (messages.value[messageIndex]) {
          const msg = messages.value[messageIndex]
          messages.value.splice(messageIndex, 1, { 
            ...msg, 
            content: currentStreamMessage.value, 
            isStreaming: true 
          })
          console.log(`[DELTA事件] UI更新完成，显示长度: ${currentStreamMessage.value.length}`)
          await nextTick()
        }
        
        // 优化滚动，使用防抖
        if (scrollTimer) {
          clearTimeout(scrollTimer)
        }
        scrollTimer = window.setTimeout(() => {
          scrollToBottom()
          scrollTimer = null
        }, 50) // 增加防抖时间，避免过于频繁的滚动
      }
      break
      
    case 'complete':
      console.log('[COMPLETE事件] 回复完成:', eventData.message, 'recordId:', eventData.recordId)
      console.log(`[COMPLETE事件] 最终内容长度: ${currentStreamMessage.value.length}`)
      isStreaming.value = false
      
      // 清理防抖定时器
      if (scrollTimer) {
        clearTimeout(scrollTimer)
        scrollTimer = null
      }
      
      if (messages.value[messageIndex]) {
        // 获取完整的回复内容
        const completeContent = currentStreamMessage.value
        
        console.log('[COMPLETE事件] 设置最终消息内容')
        console.log(`[COMPLETE事件] 完整内容长度: ${completeContent.length}`)
        
        // 直接更新消息内容和状态
        messages.value[messageIndex].content = completeContent
        messages.value[messageIndex].isStreaming = false
        if (eventData.recordId) {
          messages.value[messageIndex].rid = eventData.recordId
        }
        
        // 使用nextTick确保DOM更新完成后再进行下一步操作
        await nextTick()
        console.log('[COMPLETE事件] Vue DOM更新完成，MessageBubble应该已重新渲染markdown')
        // 最终滚动到底部
        scrollToBottom()
      }
      currentStreamMessage.value = '' // 重置累积内容
      break
      
    case 'error':
      console.error('[ERROR事件] AI回复错误:', eventData.message || eventData.error)
      const errorMsg = eventData.message || eventData.error || '未知错误'
      ElMessage.error(`AI回复出错: ${errorMsg}`)
      isStreaming.value = false
      if (messages.value[messageIndex]) {
        messages.value[messageIndex].content = `❌ 错误: ${errorMsg}`
        messages.value[messageIndex].isError = true
        delete messages.value[messageIndex].isStreaming
      }
      currentStreamMessage.value = '' // 重置累积内容
      break
      
    default:
      console.log('[未知事件] 类型:', eventData.type, '数据:', eventData)
  }
}

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
  if (messages.value.length === 1) {
    const message = messages.value[0].content
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
            @update:input-message="(val: string) => inputMessage = val"
            @send-message="handleSendMessage"
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