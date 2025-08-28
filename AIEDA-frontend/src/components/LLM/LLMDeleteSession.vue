<template>
  <!-- 这里不需要实现具体的HTML，只需要脚本逻辑 -->
  <div></div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { sendMessageInput } from '../../api/chat'

// 定义props
const props = defineProps<{
  uid: number
  sid: number
}>()

// 定义emits
const emit = defineEmits<{
  'delete-confirmed': []
}>()

// 响应式数据
const deleting = ref(false)

// 发送删除消息到后端
const handleDeleteSession = async (): Promise<boolean> => {
  if (deleting.value) return false
  
  deleting.value = true
  
  try {
    // 构造删除请求数据
    const requestData = {
      uid: props.uid,
      sid: props.sid,
      content: "",
      inputType: "delete" as "delete"
    }

    console.log('发送删除会话请求:', requestData)
    
    // 使用非流式接口发送删除请求
    const response = await sendMessageInput(requestData)

    if (response.code !== '200') {
      throw new Error(`删除请求失败: ${response.message}`)
    }

    console.log('删除请求发送成功')
    
    // 发出删除确认事件
    emit('delete-confirmed')
    
    return true
    
  } catch (error) {
    console.error('发送删除请求失败:', error)
    ElMessage.error('删除请求发送失败')
    return false
  } finally {
    deleting.value = false
  }
}

// 暴露组件方法
defineExpose({
  handleDeleteSession,
  deleting
})
</script>

<style scoped>
/* 不需要具体样式 */
</style>
