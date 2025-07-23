<!-- ChatAside.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { ElButton, ElIcon, ElScrollbar } from 'element-plus'
import { Plus, Operation } from '@element-plus/icons-vue'
import SessionItem from './SessionItem.vue'
import type { SessionRecord } from './ChatPage.vue'

// 接收父组件参数
const props = defineProps<{
  sessions: SessionRecord[]
  currentSessionId: number
  isLoading: boolean
}>()

// 传递给父组件的事件
const emit = defineEmits<{
  'create-session': []
  'select-session': [sessionId: number]
  'edit-session': [sessionId: number, newTitle: string]
  'delete-session': [sessionId: number]
}>()

const sidebarOpen = ref(true)
</script>

<template>
  <div class="chat-aside" :class="{ 'collapsed': !sidebarOpen }">
    <div class="chat-aside-header">
      <el-button 
        v-if="sidebarOpen"
        type="primary" 
        @click="emit('create-session')" 
        :loading="props.isLoading" 
        round
        style="background-color: rgb(102, 8, 116); border-color: rgb(102, 8, 116);"
      >
        <el-icon><Plus /></el-icon>
        新对话
      </el-button>
      <el-button 
        plain 
        round 
        @click="sidebarOpen = !sidebarOpen" 
        :icon="sidebarOpen ? Operation : Operation" 
        class="sidebar-toggle"
        style="color: rgb(102, 8, 116); border-color: rgb(102, 8, 116);"
        :title="sidebarOpen ? '收起侧边栏' : '展开侧边栏'"
      />
    </div>
    
    <el-scrollbar class="chat-sessions">
      <div class="session-list" v-if="sidebarOpen">
        <SessionItem 
          v-for="session in props.sessions" 
          :key="session.sid"
          :session="session"
          :is-active="session.sid === props.currentSessionId"
          @select="() => emit('select-session', session.sid)"
          @edit="(newTitle: string) => emit('edit-session', session.sid, newTitle)"
          @delete="() => emit('delete-session', session.sid)"
        />
      </div>
      <!-- 收起状态下的简化显示 -->
      <div class="session-list-collapsed" v-else>
        <div 
          v-for="session in props.sessions.slice(0, 5)" 
          :key="session.sid"
          class="session-dot"
          :class="{ 'active': session.sid === props.currentSessionId }"
          @click="emit('select-session', session.sid)"
          :title="session.title"
        ></div>
      </div>
    </el-scrollbar>
  </div>
</template>

<style scoped>
.chat-aside {
  width: 280px;
  height: 100%;
  border-right: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.chat-aside.collapsed {
  width: 80px;
}

.chat-aside-header {
  padding: 1rem;
  display: flex;
  gap: 8px;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
}

.sidebar-toggle {
  flex-shrink: 0;
  min-width: 40px;
}

.chat-sessions {
  height: calc(100% - 90px);
}

.session-list {
  padding: 0;
}

.session-list-collapsed {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.session-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: rgba(102, 8, 116, 0.3);
  cursor: pointer;
  transition: all 0.2s ease;
}

.session-dot:hover {
  background-color: rgba(102, 8, 116, 0.6);
  transform: scale(1.2);
}

.session-dot.active {
  background-color: rgb(102, 8, 116);
  transform: scale(1.3);
}
</style>