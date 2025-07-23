<!-- SessionItem.vue -->
<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElIcon, ElButton, ElInput, ElMessageBox, ElMessage } from 'element-plus'
import { Edit, Delete, ChatLineSquare } from '@element-plus/icons-vue'
import { updateSessionTitle, deleteSession } from '../../api/chat'
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
    class="session-container"
    :class="{ 'is-active': isActive }"
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
}

.session-container:hover {
  background-color: rgba(102, 8, 116, 0.08);
}

.session-container.is-active {
  background-color: rgba(102, 8, 116, 0.12);
  border-left: 3px solid rgb(102, 8, 116);
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
}

.session-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #333;
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
}

.delete-btn {
  color: #f56c6c !important;
}

.delete-btn:hover {
  background-color: rgba(245, 108, 108, 0.1) !important;
}
</style>