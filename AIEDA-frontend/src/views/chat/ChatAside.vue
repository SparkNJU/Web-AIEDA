<!-- ChatAside.vue -->
<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { ElButton, ElIcon, ElScrollbar } from 'element-plus'
import { Plus, Operation } from '@element-plus/icons-vue'
import SessionItem from './SessionItem.vue'
import type { SessionRecord } from './ChatPage.vue'

// 接收父组件参数
const props = defineProps<{
  sessions: SessionRecord[]
  currentSessionId: number
  isLoading: boolean
  forceCollapsed?: boolean // 新增：强制收起状态
}>()

// 传递给父组件的事件
const emit = defineEmits<{
  'create-session': []
  'select-session': [sessionId: number]
  'edit-session': [sessionId: number, newTitle: string]
  'delete-session': [sessionId: number]
}>()

const sidebarOpen = ref(false) // 默认收起

// 响应式检测屏幕大小
const isMobile = ref(window.innerWidth <= 768)

// 监听窗口大小变化
const handleResize = () => {
  const wasMobile = isMobile.value
  isMobile.value = window.innerWidth <= 768
  
  // 当从桌面端切换到移动端时，强制收起侧边栏
  if (!wasMobile && isMobile.value) {
    sidebarOpen.value = false
  }
  // 当从移动端切换到桌面端时，展开侧边栏
  else if (wasMobile && !isMobile.value) {
    sidebarOpen.value = true
  }
}

// 挂载时初始化
onMounted(() => {
  isMobile.value = window.innerWidth <= 768
  // 初始状态：移动端收起，桌面端展开
  sidebarOpen.value = !isMobile.value
  window.addEventListener('resize', handleResize)
})

// 卸载时清理事件监听器
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// 监听强制收起状态
watch(() => props.forceCollapsed, (newValue: boolean | undefined) => {
  if (newValue !== undefined) {
    sidebarOpen.value = !newValue
  }
})
</script>

<template>
  <div class="chat-aside chat-sidebar chat-theme" :class="{ 'collapsed': !sidebarOpen }">
    <div class="chat-aside-header">
      <el-button 
        v-if="sidebarOpen"
        type="primary" 
        @click="emit('create-session')" 
        :loading="props.isLoading" 
        round
        style="background: var(--chat-primary); border-color: var(--chat-primary);"
      >
        <el-icon><Plus /></el-icon>
        新对话
      </el-button>
      <el-button 
        plain 
        size="small"
        circle
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
  width: 180px;
  height: 100%;
  border-right: 1px solid var(--chat-border);
  background: var(--chat-bg-sidebar);
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.chat-aside.collapsed {
  width: 48px; /* 相应减小收起状态的宽度 (80 * 0.6 = 48) */
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
  min-width: 22px;
  width: 22px;
  height: 22px;
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

/* 移动端优化 */
@media (max-width: 768px) {
  .chat-aside {
    width: 140px; /* 移动端展开时更小 */
    position: absolute; /* 改为绝对定位，覆盖聊天区域 */
    left: 0;
    top: 0;
    z-index: 1000;
    background: var(--chat-bg-sidebar);
    backdrop-filter: blur(10px);
    border-right: 1px solid var(--chat-border);
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
    transform: translateX(-100%); /* 默认隐藏 */
    transition: transform 0.3s ease;
  }
  
  .chat-aside:not(.collapsed) {
    transform: translateX(0); /* 展开时显示 */
  }
  
  .chat-aside.collapsed {
    width: 32px; /* 移动端收起时更小 */
    transform: translateX(0); /* 收起状态始终显示 */
    background: var(--chat-bg-sidebar);
    backdrop-filter: blur(5px);
  }
  
  .chat-aside-header {
    padding: 8px; /* 移动端大幅减少内边距 */
    min-height: 48px;
    flex-direction: column;
    gap: 4px;
  }
  
  .chat-aside-header .el-button:not(.sidebar-toggle) {
    font-size: 0.7rem; /* 移动端按钮字体更小 */
    padding: 4px 8px; /* 移动端按钮内边距更小 */
    height: 28px;
    min-height: 28px;
  }
  
  .sidebar-toggle {
    min-width: 24px;
    width: 24px;
    height: 24px;
    padding: 0;
    position: absolute;
    top: 8px;
    right: 4px;
  }
  
  .chat-sessions {
    height: calc(100% - 60px); /* 移动端调整高度 */
    padding: 0 4px; /* 增加侧边内边距 */
  }
  
  .session-list {
    padding: 4px 0; /* 移动端减少会话列表内边距 */
  }
  
  .session-list-collapsed {
    padding: 4px 2px; /* 移动端减少内边距 */
    gap: 4px; /* 移动端减少间距 */
  }
  
  .session-dot {
    width: 8px; /* 移动端圆点更小 */
    height: 8px;
  }
  
  /* 移动端遮罩层 - 展开时添加背景遮罩 */
  .chat-aside:not(.collapsed)::before {
    content: '';
    position: fixed;
    top: 0;
    left: 140px;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: -1;
    pointer-events: auto;
  }
}
</style>