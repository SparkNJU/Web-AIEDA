<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { sendMessageInput, stopSessionTimeout, restartSessionTimeout } from '../../api/chat'

// 定义干预状态类型
export type InterventionState = 'normal' | 'streaming' | 'paused' | 'instruct'

// 接收参数
const props = defineProps<{
  state: InterventionState
  hasInput: boolean
  uid: number
  sid: number
}>()

// 事件传递
const emit = defineEmits<{
  'pause-streaming': []
  'send-instruction': [instruction: string]
  'resume-streaming': []
}>()

// 响应式数据
const processing = ref(false)

// 处理硬干预（暂停流式输出）
const handleHardIntervention = async (): Promise<boolean> => {
  if (processing.value) return false
  
  processing.value = true
  
  try {
    // 1. 首先停止SSE会话超时计时
    console.log('停止会话超时计时:', props.sid)
    const timeoutResponse = await stopSessionTimeout(props.sid)
    
    if (!timeoutResponse || timeoutResponse.data.code !== '200') {
      throw new Error('停止超时计时失败')
    }
    
    console.log('超时计时已停止')
    
    // 2. 发送硬干预信号到后端（使用非流式接口）
    const requestData = {
      uid: props.uid,
      sid: props.sid,
      content: "",
      inputType: "intervention" as "intervention",
      metadata: {
        type: "hard"
      }
    }

    console.log('发送硬干预信号:', requestData)
    
    // 使用非流式接口发送硬干预信号
    const response = await sendMessageInput(requestData)

    if (response.code !== '200') {
      throw new Error(`硬干预请求失败: ${response.message}`)
    }

    console.log('硬干预信号发送成功')
    
    // 发出暂停事件
    emit('pause-streaming')
    
    return true
    
  } catch (error) {
    console.error('硬干预失败:', error)
    ElMessage.error('暂停失败，请重试')
    return false
  } finally {
    processing.value = false
  }
}

// 处理暂停流式输出
const handlePause = () => {
  handleHardIntervention()
}

// 处理软干预（发送指令）
const handleSoftIntervention = async (instruction: string): Promise<boolean> => {
  if (processing.value) return false
  
  processing.value = true
  
  try {
    // 如果当前是paused状态，需要重启超时计时
    if (props.state === 'paused') {
      console.log('从paused状态发送软干预，重启超时计时:', props.sid)
      const restartResponse = await restartSessionTimeout(props.sid)
      
      if (!restartResponse || restartResponse.data.code !== '200') {
        console.warn('重启超时计时失败，但继续发送软干预')
      } else {
        console.log('超时计时已重启')
      }
    }
    
    // 发送软干预信号到后端（使用非流式接口）
    const requestData = {
      uid: props.uid,
      sid: props.sid,
      content: instruction,
      inputType: "intervention" as "intervention",
      metadata: {
        type: "soft"
      }
    }

    console.log('发送软干预信号:', requestData)
    
    // 使用非流式接口发送软干预信号
    const response = await sendMessageInput(requestData)

    if (response.code !== '200') {
      throw new Error(`软干预请求失败: ${response.message}`)
    }

    console.log('软干预信号发送成功')
    
    // 发出指令事件
    emit('send-instruction', instruction)
    
    return true
    
  } catch (error) {
    console.error('软干预失败:', error)
    ElMessage.error('发送指令失败，请重试')
    return false
  } finally {
    processing.value = false
  }
}

// 处理发送指令
const handleInstruct = (instruction: string) => {
  handleSoftIntervention(instruction)
}

// 根据状态和输入确定按钮显示
const getButtonState = () => {
  if (props.state === 'streaming') {
    return props.hasInput ? 'instruct' : 'pause'
  }
  if (props.state === 'paused') {
    return 'instruct'
  }
  return 'send'
}

// 根据按钮状态获取样式
const getButtonStyle = () => {
  const buttonState = getButtonState()
  
  switch (buttonState) {
    case 'pause':
      return {
        backgroundColor: '#ef4444',
        borderColor: '#ef4444',
        color: '#ffffff'
      }
    case 'instruct':
      return {
        backgroundColor: '#ffffff',
        borderColor: '#d1d5db',
        color: '#374151'
      }
    default: // send
      return {
        backgroundColor: '#22c55e',
        borderColor: '#22c55e',
        color: '#ffffff'
      }
  }
}

// 根据按钮状态获取图标类型
const getButtonIcon = () => {
  const buttonState = getButtonState()
  
  switch (buttonState) {
    case 'pause':
      return 'pause'
    case 'instruct':
      return 'arrow-up'
    default: // send
      return 'play'
  }
}

// 根据按钮状态获取文本 (现在不显示文本，但保留用于title)
const getButtonText = () => {
  const buttonState = getButtonState()
  
  switch (buttonState) {
    case 'pause':
      return '暂停'
    case 'instruct':
      return 'Instruct'
    default:
      return '发送'
  }
}

// 根据按钮状态获取点击处理函数
const getClickHandler = (inputMessage: string) => {
  const buttonState = getButtonState()
  
  return () => {
    switch (buttonState) {
      case 'pause':
        handlePause()
        break
      case 'instruct':
        handleInstruct(inputMessage)
        break
      default:
        // 这个情况由父组件的sendMessage处理
        break
    }
  }
}

// 暴露方法给父组件
defineExpose({
  getButtonState,
  getButtonStyle,
  getButtonText,
  getButtonIcon,
  getClickHandler,
  handleHardIntervention,
  handleSoftIntervention,
  processing
})
</script>

<template>
  <div class="llm-intervention">
    <!-- 此组件主要提供逻辑，UI由父组件渲染 -->
  </div>
</template>

<style scoped>
.llm-intervention {
  display: none; /* 此组件不渲染UI，只提供逻辑 */
}
</style>
