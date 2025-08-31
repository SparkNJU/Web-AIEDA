<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMenu, ElMenuItem, ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'

// 定义props
const props = defineProps<{
  message: string // 用户确认的消息内容
  uid: number
  sid: number
  visible: boolean // 控制显示/隐藏
}>()

// 定义事件
const emit = defineEmits<{
  'send-confirmation': [choice: '1' | '2'] // 1-确认，2-取消
}>()

// 响应式数据
const isProcessing = ref(false) // 是否正在处理中
const hasResponded = ref(false) // 是否已经响应过

// 监听visible变化，重置状态
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    hasResponded.value = false
    isProcessing.value = false
  }
})

// 处理确认选择
const handleConfirm = async () => {
  if (isProcessing.value || hasResponded.value) return
  
  isProcessing.value = true
  hasResponded.value = true
  
  try {
    emit('send-confirmation', '1')
    ElMessage.success('已确认')
  } catch (error) {
    console.error('发送确认失败:', error)
    hasResponded.value = false
  } finally {
    isProcessing.value = false
  }
}

// 处理取消选择
const handleCancel = async () => {
  if (isProcessing.value || hasResponded.value) return
  
  isProcessing.value = true
  hasResponded.value = true
  
  try {
    emit('send-confirmation', '2')
    ElMessage.success('已取消')
  } catch (error) {
    console.error('发送取消失败:', error)
    hasResponded.value = false
  } finally {
    isProcessing.value = false
  }
}

// 获取菜单项的样式类
const getMenuItemClass = (type: 'confirm' | 'cancel') => {
  const baseClass = 'confirmation-menu-item'
  if (hasResponded.value) {
    return `${baseClass} ${baseClass}--disabled`
  }
  return `${baseClass} ${baseClass}--${type}`
}
</script>

<template>
  <div v-if="visible" class="user-confirmation">
    <!-- 确认消息描述 -->
    <div class="confirmation-message">
      {{ message }}
    </div>
    
    <!-- 确认选择菜单 -->
    <el-menu
      mode="horizontal"
      class="confirmation-menu"
      :disabled="hasResponded"
    >
      <el-menu-item
        index="confirm"
        :class="getMenuItemClass('confirm')"
        :disabled="hasResponded || isProcessing"
        @click="handleConfirm"
      >
        <el-icon><Check /></el-icon>
        <span>确认</span>
      </el-menu-item>
      
      <el-menu-item
        index="cancel"
        :class="getMenuItemClass('cancel')"
        :disabled="hasResponded || isProcessing"
        @click="handleCancel"
      >
        <el-icon><Close /></el-icon>
        <span>取消</span>
      </el-menu-item>
    </el-menu>
    
    <!-- 处理状态提示 -->
    <div v-if="hasResponded" class="status-hint">
      已响应，等待处理...
    </div>
  </div>
</template>

<style scoped>
.user-confirmation {
  margin-top: 12px;
  padding: 16px;
  background: #f8f9fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.confirmation-message {
  font-size: 14px;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.5;
  font-weight: 500;
}

.confirmation-menu {
  background: transparent;
  border: none;
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 覆盖Element Plus的默认菜单样式 */
:deep(.el-menu.el-menu--horizontal) {
  border-bottom: none;
}

:deep(.el-menu-item) {
  border: none !important;
  background: transparent !important;
  color: inherit !important;
  padding: 8px 16px !important;
  border-radius: 6px !important;
  margin: 0 6px !important;
  transition: all 0.2s ease !important;
  min-width: 80px !important;
  justify-content: center !important;
  font-size: 14px !important;
}

/* 确认按钮样式 - 绿色 */
:deep(.confirmation-menu-item--confirm .el-menu-item) {
  background: #67c23a !important;
  color: white !important;
  border: 2px solid #67c23a !important;
  font-weight: 600 !important;
}

:deep(.confirmation-menu-item--confirm .el-menu-item:hover) {
  background: #85ce61 !important;
  border-color: #85ce61 !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.4) !important;
}

:deep(.confirmation-menu-item--confirm .el-menu-item:active) {
  background: #5daf34 !important;
  border-color: #5daf34 !important;
  transform: translateY(0) !important;
}

/* 取消按钮样式 - 红色 */
:deep(.confirmation-menu-item--cancel .el-menu-item) {
  background: #f56c6c !important;
  color: white !important;
  border: 2px solid #f56c6c !important;
  font-weight: 600 !important;
}

:deep(.confirmation-menu-item--cancel .el-menu-item:hover) {
  background: #f78989 !important;
  border-color: #f78989 !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.4) !important;
}

:deep(.confirmation-menu-item--cancel .el-menu-item:active) {
  background: #f25c5c !important;
  border-color: #f25c5c !important;
  transform: translateY(0) !important;
}

/* 已响应状态的禁用样式 */
:deep(.confirmation-menu-item--disabled .el-menu-item) {
  background: #e4e7ed !important;
  color: #a8abb2 !important;
  border: 2px solid #e4e7ed !important;
  cursor: not-allowed !important;
  font-weight: normal !important;
}

:deep(.confirmation-menu-item--disabled .el-menu-item:hover) {
  background: #e4e7ed !important;
  border-color: #e4e7ed !important;
  transform: none !important;
  box-shadow: none !important;
}

.status-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
  font-style: italic;
}

/* 图标和文字的间距 */
:deep(.el-menu-item .el-icon) {
  margin-right: 6px;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-confirmation {
    padding: 12px;
  }
  
  .confirmation-message {
    font-size: 13px;
    margin-bottom: 10px;
  }
  
  :deep(.el-menu-item) {
    min-width: 70px !important;
    padding: 6px 12px !important;
    font-size: 13px !important;
  }
  
  :deep(.el-menu-item .el-icon) {
    font-size: 14px;
  }
}
</style>