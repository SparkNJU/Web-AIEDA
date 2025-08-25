<script setup lang="ts">
// 定义干预状态类型
export type InterventionState = 'normal' | 'streaming' | 'paused' | 'instruct'

// 接收参数
const props = defineProps<{
  state: InterventionState
  hasInput: boolean
}>()

// 事件传递
const emit = defineEmits<{
  'pause-streaming': []
  'send-instruction': [instruction: string]
  'resume-streaming': []
}>()

// 处理暂停流式输出
const handlePause = () => {
  emit('pause-streaming')
}

// 处理发送指令
const handleInstruct = (instruction: string) => {
  emit('send-instruction', instruction)
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
  getClickHandler
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
