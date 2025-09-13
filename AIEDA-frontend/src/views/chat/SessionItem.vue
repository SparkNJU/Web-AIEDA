<!-- SessionItem.vue -->
<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElIcon, ElButton, ElInput, ElMessageBox, ElMessage } from 'element-plus'
import { Edit, Delete, ChatLineSquare } from '@element-plus/icons-vue'
import { updateSessionTitle, deleteSession } from '../../api/chat'
import LLMDeleteSession from '../../components/LLM/LLMDeleteSession.vue'
import type { SessionRecord } from './ChatPage.vue'

// 接收参数
const props = defineProps<{
  session: SessionRecord
  isActive: boolean
}>()

// 事件传递
const emit = defineEmits<{
  select: []
  edit: [newTitle: string]
  delete: []
}>()

// 编辑状态管理
const isEditing = ref(false)
const editingTitle = ref(props.session.title)

// LLMDeleteSession组件引用
const deleteSessionRef = ref<InstanceType<typeof LLMDeleteSession>>()

// 获取用户ID
const getUserId = () => Number(sessionStorage.getItem('uid') || '0')

// 编辑逻辑
const handleEdit = (e: Event) => {
  e.stopPropagation()
  isEditing.value = true
  editingTitle.value = props.session.title
  nextTick(() => {
    const input = document.getElementById(`session-input-${props.session.sid}`) as HTMLInputElement
    input?.focus()
  })
}

const saveEdit = async () => {
  const newTitle = editingTitle.value.trim()
  if (newTitle && newTitle !== props.session.title) {
    try {
      // 从sessionStorage获取用户ID
      const uid = sessionStorage.getItem('uid')
      if (!uid) {
        ElMessage.error('请先登录')
        return
      }
      
      await updateSessionTitle(Number(uid), props.session.sid, newTitle)
      emit('edit', newTitle)
      ElMessage.success('会话标题更新成功')
    } catch (error) {
      console.error('更新会话标题失败:', error)
      ElMessage.error('更新会话标题失败')
    }
  }
  isEditing.value = false
}

const cancelEdit = () => {
  editingTitle.value = props.session.title
  isEditing.value = false
}

// 删除逻辑
const handleDelete = async (e: Event) => {
  e.stopPropagation()
  try {
    await ElMessageBox.confirm(
      '确定要删除这个会话吗？此操作不可恢复。',
      '删除会话',
      { 
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    
    try {
      // 从sessionStorage获取用户ID
      const uid = sessionStorage.getItem('uid')
      if (!uid) {
        ElMessage.error('请先登录')
        return
      }
      
      // 先调用LLMDeleteSession的方法发送删除消息到后端
      if (deleteSessionRef.value) {
        const deleteResult = await deleteSessionRef.value.handleDeleteSession()
        if (!deleteResult) {
          ElMessage.error('发送删除请求失败')
          return
        }
      }
      
      await deleteSession(props.session.sid, Number(uid))
      emit('delete')
      ElMessage.success('会话删除成功')
    } catch (error) {
      console.error('删除会话失败:', error)
      ElMessage.error('删除会话失败')
    }
  } catch {
    // 用户取消删除
  }
}
</script>

<template>
  <div 
    @click="emit('select')" 
    class="session-container session-item chat-theme"
    :class="{ 'is-active': isActive, 'active': isActive }"
  >
    <!-- 非编辑状态 -->
    <template v-if="!isEditing">
      <div class="session-info">
        <el-icon class="session-icon" style="color: rgb(102, 8, 116);">
          <ChatLineSquare />
        </el-icon>
        <span class="session-title">{{ session.title || '新会话' }}</span>
      </div>
      <div class="session-actions">
        <el-button 
          type="text" 
          size="small" 
          @click="handleEdit"
          :icon="Edit"
          style="color: rgb(102, 8, 116);"
        />
        <el-button 
          type="text" 
          size="small" 
          @click="handleDelete"
          :icon="Delete"
          class="delete-btn"
        />
      </div>
    </template>

    <!-- 编辑状态 -->
    <template v-else>
      <el-input 
        v-model="editingTitle" 
        size="small"
        :id="`session-input-${session.sid}`"
        @keyup.enter="saveEdit"
        @keyup.esc="cancelEdit"
        @blur="saveEdit"
        @click.stop
      />
    </template>
  </div>
  
  <!-- LLMDeleteSession组件 -->
  <LLMDeleteSession 
    ref="deleteSessionRef"
    :uid="getUserId()"
    :sid="session.sid"
  />
</template>

<style scoped>
.session-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 10px 16px;
  cursor: pointer;
  border-radius: 6px;
  margin: 2px 8px;
  transition: all 0.2s ease;
  background: transparent;
}

.session-container:hover {
  background: var(--chat-hover-bg);
}

.session-container.is-active {
  background: var(--chat-active-bg);
  border-left: 3px solid var(--chat-primary);
}

.session-info {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
}

.session-icon {
  margin-right: 8px;
  flex-shrink: 0;
  color: var(--chat-primary);
}

.session-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--chat-text-primary);
  font-size: 0.9rem;
}

.session-actions {
  display: none;
  gap: 2px;
  opacity: 0.7;
}

.session-container:hover .session-actions,
.session-container.is-active .session-actions {
  display: flex;
}

.session-actions .el-button {
  padding: 4px;
  min-height: auto;
  width: 28px;
  height: 28px;
  color: var(--chat-text-secondary);
  background: transparent;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.session-actions .el-button:hover {
  background: var(--chat-hover-bg);
  border-color: var(--chat-border);
}

.delete-btn {
  color: var(--chat-error) !important;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.1) !important;
  border-color: var(--chat-error) !important;
}

/* 夜间模式按钮样式 */
[data-theme="dark"] .session-actions .el-button {
  color: #ffffff !important;
}

[data-theme="dark"] .delete-btn {
  color: #ffcccb !important;
}

[data-theme="dark"] .session-icon {
  color: #ffffff !important;
}

/* Element Plus输入框样式覆盖 */
:deep(.el-input__wrapper) {
  background: var(--chat-bg-input);
  border-color: var(--chat-border);
  color: var(--chat-text-primary);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: var(--chat-primary);
  box-shadow: 0 0 0 2px var(--chat-primary-light);
}

:deep(.el-input__inner) {
  color: var(--chat-text-primary);
}

:deep(.el-input__inner)::placeholder {
  color: var(--chat-text-muted);
}
</style>