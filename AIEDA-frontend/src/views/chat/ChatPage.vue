<!-- ChatPage.vue -->
<script setup lang="ts">
import { ref, nextTick, computed, onMounted, triggerRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElSwitch } from 'element-plus'
// 导入子组件
import ChatAside from './ChatAside.vue'
import MessageList from './MessageList.vue'
import ChatInput from './ChatInput.vue'
import WelcomeCard from './WelcomeCard.vue'
// 导入API
import { createSession, getSessionRecords, getUserSessions, sendMessage, sendMessageStream, updateSessionTitle, deleteSession } from '../../api/chat'

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
const userId = ref<number>(1) // 固定使用用户ID 1
const currentSessionId = ref<number>(0)
const currentSessionTitle = ref('')
const sessions = ref<SessionRecord[]>([])
const messages = ref<ChatRecord[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const isStreamMode = ref(true) // 流式输出开关，默认开启
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
  return isLoading.value || isStreaming.value || currentSessionId.value === 0 || !userId.value
})

// 初始化加载
onMounted(() => {
  loadUserSessions()
})

// 加载用户会话列表
const loadUserSessions = async () => {
  try {
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
  // 添加用户消息到界面
  messages.value.push({
    content: messageToSend,
    direction: true,
    sid: currentSessionId.value
  })
  inputMessage.value = ''
  scrollToBottom()
  updateSessionTime()

  // 根据流式模式选择不同的发送方式
  if (isStreamMode.value) {
    await handleSendMessageStream(messageToSend)
  } else {
    await handleSendMessageNormal(messageToSend)
  }
}

// 普通消息发送（原有逻辑）
const handleSendMessageNormal = async (messageToSend: string) => {
  isLoading.value = true
  try {
    const res = await sendMessage({
      uid: userId.value,
      content: messageToSend,
      sid: currentSessionId.value
    })
    
    if (res.data.code === '200') {
      // 添加AI回复到界面
      const aiReply = res.data.data
      messages.value.push({
        rid: aiReply.rid,
        sid: aiReply.sid,
        direction: aiReply.direction,
        content: aiReply.content,
        sequence: aiReply.sequence,
        type: aiReply.type,
        createTime: aiReply.createTime
      })
      scrollToBottom()
    } else {
      ElMessage.error(res.data.message || '发送消息失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
  } finally {
    isLoading.value = false
  }
}

// SSE流式消息发送
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
    const requestData = {
      uid: userId.value,
      sid: currentSessionId.value,
      content: messageToSend
    }

    const response = await sendMessageStream(requestData)
    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法获取响应流')
    }

    const decoder = new TextDecoder()
    
    const readStream = async (): Promise<void> => {
      const { done, value } = await reader.read()
      
      if (done) {
        isStreaming.value = false
        // 确保移除流式标记，使用数组更新方式
        if (messages.value[aiMessageIndex]) {
          const newMessages = [...messages.value]
          newMessages[aiMessageIndex] = {
            ...newMessages[aiMessageIndex],
            isStreaming: false
          }
          messages.value = newMessages
          
          // 强制触发Vue的响应式更新
          triggerRef(messages)
          
          console.log('SSE流读取完成，已停止流式状态')
          
          // 如果没有收到complete事件，手动触发
          if (currentStreamMessage.value.length > 0) {
            console.log('流结束但未收到complete事件，手动触发complete处理')
            handleSSEEvent({
              type: 'complete',
              message: '回复完成',
              recordId: -1
            }, aiMessageIndex)
          }
        }
        return
      }
      
      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')
      
      for (const line of lines) {
        const trimmedLine = line.trim()
        if (!trimmedLine) continue
        
        // 解析SSE格式: event:xxx 和 data:xxx
        if (trimmedLine.startsWith('event:')) {
          // 当前事件类型，可以用于调试
          const eventType = trimmedLine.substring(6).trim()
          console.log('SSE Event:', eventType)
        } else if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.substring(5).trim()
          console.log('SSE Data:', data)
          
          // 检查data是否为空或只包含空白字符
          if (data && data !== '[DONE]' && data !== '{}' && data.length > 0) {
            try {
              const eventData = JSON.parse(data)
              // 只处理有效的事件数据
              if (eventData && eventData.type) {
                console.log('解析后的事件数据:', eventData)
                handleSSEEvent(eventData, aiMessageIndex)
              } else {
                console.log('跳过无效事件数据:', eventData)
              }
            } catch (e) {
              console.warn('Failed to parse SSE data:', data, e)
              // 对于无法解析的数据，不处理但不中断流程
            }
          } else {
            console.log('跳过空的data:', data)
          }
        }
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
const handleSSEEvent = (eventData: any, messageIndex: number) => {
  console.log('处理SSE事件:', eventData)
  
  switch (eventData.type) {
    case 'start':
      console.log('AI开始思考:', eventData.message)
      // 显示思考状态，直接更新内容并确保isStreaming为true
      if (messages.value[messageIndex]) {
        const newMessages = [...messages.value]
        newMessages[messageIndex] = {
          ...newMessages[messageIndex],
          content: eventData.message || '🤔 AI正在思考...',
          isStreaming: true // 确保设置为流式状态
        }
        messages.value = newMessages
        
        // 强制触发Vue的响应式更新
        triggerRef(messages)
        
        console.log('更新思考状态:', messages.value[messageIndex].content)
        console.log('isStreaming 设置为 true')
      }
      scrollToBottom()
      break
      
    case 'delta':
    case 'message': // 兼容后端返回的message事件类型
      console.log('AI回复片段:', eventData.content)
      console.log('当前累积内容长度:', currentStreamMessage.value.length)
      
      // 处理可能的JSON格式内容
      let contentToAdd = eventData.content || ''
      try {
        // 尝试解析JSON格式的内容
        const parsed = JSON.parse(contentToAdd)
        if (parsed.answer) {
          contentToAdd = parsed.answer
          console.log('解析JSON内容:', contentToAdd)
        }
      } catch (e) {
        // 如果不是JSON格式，直接使用原内容
        console.log('非JSON格式，直接使用:', contentToAdd)
      }
      
      // 如果是第一个内容片段，清除思考提示和连接中状态
      if (messages.value[messageIndex] && 
          (messages.value[messageIndex].content.includes('思考') || 
           messages.value[messageIndex].content.includes('🤔') ||
           messages.value[messageIndex].content.includes('⏳') ||
           messages.value[messageIndex].content.includes('连接中'))) {
        currentStreamMessage.value = ''
        console.log('清除思考状态，重置累积内容')
      }
      
      // 累加内容，实现流式显示
      currentStreamMessage.value += contentToAdd
      console.log('更新后累积内容长度:', currentStreamMessage.value.length)
      
      // 实时更新消息内容
      if (messages.value[messageIndex]) {
        // 只使用数组替换方法，确保Vue能检测到所有变化
        const newMessages = [...messages.value]
        newMessages[messageIndex] = {
          ...newMessages[messageIndex],
          content: currentStreamMessage.value,
          isStreaming: true // 确保在流式过程中保持streaming状态
        }
        messages.value = newMessages
        
        // 强制触发Vue的响应式更新
        triggerRef(messages)
        
        console.log(`实时更新第${messageIndex}条消息，内容长度:`, currentStreamMessage.value.length)
      }
      
      // 优化滚动，使用更短的防抖时间以提高响应性
      if (scrollTimer) {
        clearTimeout(scrollTimer)
      }
      scrollTimer = window.setTimeout(() => {
        scrollToBottom()
        scrollTimer = null
      }, 30) // 减少到30ms防抖，提高实时性
      break
      
    case 'complete':
      console.log('回复完成:', eventData.message, 'recordId:', eventData.recordId)
      isStreaming.value = false
      
      // 清理防抖定时器
      if (scrollTimer) {
        clearTimeout(scrollTimer)
        scrollTimer = null
      }
      
      if (messages.value[messageIndex]) {
        // 获取完整的回复内容
        const completeContent = currentStreamMessage.value
        
        // 流式输出完成后，重新设置内容以触发MessageBubble的markdown重新渲染
        console.log('流式输出完成，准备重新渲染markdown内容')
        console.log('完整内容长度:', completeContent.length)
        console.log('完整内容:', completeContent)
        
        // 使用数组替换方式确保Vue能检测到所有变化
        const newMessages = [...messages.value]
        newMessages[messageIndex] = {
          ...newMessages[messageIndex],
          content: completeContent, // 确保使用完整的流式内容
          rid: eventData.recordId || newMessages[messageIndex].rid,
          isStreaming: false // 明确设置为false，停止流式指示器
        }
        messages.value = newMessages
        
        // 强制触发Vue的响应式更新，确保MessageBubble重新渲染
        triggerRef(messages)
        
        // 使用nextTick确保DOM更新完成后再进行下一步操作
        nextTick(() => {
          console.log('Vue DOM更新完成，MessageBubble应该已重新渲染markdown')
          // 最终滚动到底部
          scrollToBottom()
        })
        
        console.log('最终消息内容长度:', completeContent.length)
        console.log('isStreaming 已设置为 false，将触发MessageBubble的md.render重新渲染')
      }
      currentStreamMessage.value = ''
      break
      
    case 'error':
      console.error('AI回复错误:', eventData.message || eventData.error)
      const errorMsg = eventData.message || eventData.error || '未知错误'
      ElMessage.error(`AI回复出错: ${errorMsg}`)
      isStreaming.value = false
      if (messages.value[messageIndex]) {
        messages.value[messageIndex].content = `❌ 错误: ${errorMsg}`
        messages.value[messageIndex].isError = true
        delete messages.value[messageIndex].isStreaming
      }
      currentStreamMessage.value = ''
      break
      
    default:
      console.log('未知事件类型:', eventData.type, eventData)
  }
}

// 工具方法
const updateSessionTime = () => {
  const session = sessions.value.find(s => s.sid === currentSessionId.value)
  if (!session) return
  
  session.updateTime = new Date().toISOString()
  sessions.value = [...sessions.value].sort((a, b) => b.updateTime.localeCompare(a.updateTime))
  
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
          <div class="chat-main-header" v-if="currentSessionId !== 0">
            <div class="header-content">
              <h2>{{ currentSessionTitle || '新会话' }}</h2>
              <div class="header-controls">
                <div class="stream-toggle">
                  <el-switch
                    v-model="isStreamMode"
                    :disabled="isLoading || isStreaming"
                    active-text="流式输出"
                    inactive-text="普通模式"
                    active-color="#660874"
                    size="small"
                  />
                </div>
              </div>
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
              @insert-question="(q: string) => { inputMessage = q; handleCreateSession() }"
            />
          </div>

          <!-- 输入区域 - 固定在底部 -->
          <ChatInput 
            :input-message="inputMessage"
            :is-loading="isLoading"
            :input-disabled="inputDisabled"
            :is-stream-mode="isStreamMode"
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
  height: 100vh;
  background-color: rgba(102, 8, 116, 0.02);
  padding-top: 80px; /* 为固定Header预留空间 */
}

.chat-content {
  height: calc(100vh - 80px);
  width: 100%;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.chat-layout {
  display: flex;
  height: 100%;
  width: 100%;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0; /* 防止内容溢出 */
}

.chat-main-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-main-header h2 {
  margin: 0;
  color: rgb(102, 8, 116);
  font-size: 1.2rem;
  font-weight: 500;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stream-toggle {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.chat-main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 允许内容收缩 */
  overflow: hidden;
}
</style>