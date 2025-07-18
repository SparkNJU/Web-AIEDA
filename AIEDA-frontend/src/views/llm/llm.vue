<template>
  <div class="llm-container">
    <div class="container">
    <!-- 头部 -->
    <header class="header">
      <div class="header-left">
        <h1>🚀 IGBT网表生成器</h1>
        <div class="connection-status" id="connectionStatus">连接中...</div>
      </div>
      <div class="header-right">
        <div class="status-indicator">
          <span class="status-dot" id="statusDot"></span>
          <span id="statusText">就绪</span>
        </div>
        <button class="theme-toggle" id="themeToggle" title="切换主题">
          <span id="themeIcon">🌙</span>
        </button>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 侧边栏切换按钮（移动端） -->
      <button class="sidebar-toggle" id="leftSidebarToggle" style="display: none;">📋</button>
      <button class="sidebar-toggle right" id="rightSidebarToggle" style="display: none;">🔧</button>
      
      <!-- 左侧会话管理侧边栏 -->
      <aside class="sidebar left-sidebar" id="leftSidebar">
        <div class="sidebar-section">
          <h3>📋 会话管理</h3>
          
          <div class="form-group">
            <button class="btn btn-primary" @click="createNewSession()">🆕 创建新会话</button>
            <div id="createSessionInfo" class="temp-info" style="display:none;"></div>
            
            <div class="session-selector">
              <label for="sessionSelect">选择会话:</label>
              <select id="sessionSelect" class="form-control" @change="switchToSelectedSession()">
                <option value="">-- 选择已有会话 --</option>
              </select>
            </div>
            
            <button class="btn btn-secondary" @click="getSessionInfo()">ℹ️ 获取会话信息</button>
            <div id="getSessionInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-secondary" @click="listAllSessions()">📄 列出所有会话</button>
            <div id="listSessionsInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-danger" @click="deleteCurrentSession()">🗑️ 删除当前会话</button>
            <div id="deleteSessionInfo" class="temp-info" style="display:none;"></div>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="fixed-label">📊 会话信息</div>
          <div class="info-display-compact">
            <pre id="sessionInfo">等待创建会话...</pre>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="fixed-label">📈 统计信息</div>
          <div class="info-display-compact">
            <pre id="sessionStats">消息数: 0
生成次数: 0
成功率: 0%</pre>
          </div>
        </div>

        <div class="divider"></div>

        <div class="sidebar-section">
          <div class="fixed-label">📁 已上传文件</div>
          <div class="form-group">
            <button class="btn btn-secondary" @click="updateSidebarFilesList()" title="刷新文件列表">🔄 刷新列表</button>
          </div>
          <div class="info-display-compact">
            <div id="uploadedFilesList">暂无上传文件</div>
          </div>
        </div>
      </aside>

      <!-- 中间主聊天区域 -->
      <main class="main-chat">
        <div class="chat-header">
          <h2>💬 智能对话生成</h2>
        </div>

        <div class="chat-messages" id="chatMessages">
          <!-- 消息将在这里动态添加 -->
        </div>

        <div class="chat-input">
          <!-- 上传文件显示区域 -->
          <div class="uploaded-files-display" id="uploadedFilesDisplay" style="display: none;"></div>
          
          <div class="input-group">
            <!-- 文件上传按钮 -->
            <button class="attachment-btn" id="attachmentBtn" title="上传文件">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66L9.64 16.2a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
              </svg>
            </button>
            
            <div class="input-wrapper">
              <textarea 
                id="userInput" 
                placeholder="请详细描述您需要的IGBT电路，例如：'生成一个单管IGBT开关电路' 或 '创建IGBT桥式整流电路网表'" 
                @keydown="handleKeyDown($event)"
                rows="1"
              ></textarea>
            </div>
            
            <button id="sendBtn">发送</button>
          </div>
          
          <!-- 隐藏的文件输入 -->
          <input type="file" id="fileUpload" multiple accept=".txt,.sp,.cir,.net,.json,.csv,.py" style="display: none;">
        </div>
      </main>

      <!-- 右侧数据管理侧边栏 -->
      <aside class="sidebar right-sidebar" id="rightSidebar">
        <div class="sidebar-section">
          <div class="fixed-label">🔧 数据管理</div>
          <div class="form-group">
            <button class="btn btn-secondary" @click="getStreamData()">📊 获取流式数据</button>
            <div id="getStreamDataInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-warning" @click="clearStreamData()">🧹 清空流式数据</button>
            <div id="clearStreamDataInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-success" @click="exportChatHistory()">📥 导出对话历史</button>
            <div id="exportChatInfo" class="temp-info" style="display:none;"></div>
          </div>
          <div class="info-display-compact">
            <pre id="dataInfo">暂无数据</pre>
          </div>
        </div>

        <div class="divider"></div>

        <div class="sidebar-section">
          <div class="fixed-label">📁 文件管理</div>
          <div class="form-group">
            <button class="btn btn-primary" id="quickDownloadBtn" @click="quickDownloadNetlist()">📄 快速下载网表</button>
            <div id="quickDownloadInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-secondary" @click="listSessionFiles()">📋 会话文件列表</button>
            <div id="listSessionFilesInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-secondary" @click="getSessionTasks()">⚙️ 获取会话任务</button>
            <div id="getSessionTasksInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-info" @click="getSessionStatus()">📊 获取会话状态</button>
            <div id="getSessionStatusInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-warning" @click="getSessionOutput()">📤 获取会话输出</button>
            <div id="getSessionOutputInfo" class="temp-info" style="display:none;"></div>
            
            <button class="btn btn-danger" @click="clearOldFiles()">🗑️ 清理旧文件</button>
            <div id="clearOldFilesInfo" class="temp-info" style="display:none;"></div>
          </div>
          <div class="info-display-compact">
            <pre id="fileInfo">暂无文件信息</pre>
          </div>
        </div>

        <div class="divider"></div>

        <div class="form-group">
          <h4>⚙️ 系统设置</h4>
          <label>
            <select id="streamMode">
              <option value="true">✅ 启用流式模式</option>
              <option value="false">❌ 禁用流式模式</option>
            </select>
          </label>
          <label>
            <select id="autoScroll">
              <option value="true">⬇️ 自动滚动</option>
              <option value="false">✋ 手动滚动</option>
            </select>
          </label>
        </div>
      </aside>
    </div>
  </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, nextTick } from 'vue'
import { useLlmIntegration, createFunctionWrapper } from '../../composables/useLlmIntegration'

// 使用LLM集成composable
const { loadScripts, cleanup } = useLlmIntegration()

onMounted(async () => {
  // 等待Vue组件完全渲染
  await nextTick()
  // 动态加载所需的JavaScript文件
  loadScripts()
})

onUnmounted(() => {
  // 清理脚本和全局变量
  cleanup()
})

// 创建函数包装器
const createNewSession = createFunctionWrapper('createNewSession')
const switchToSelectedSession = createFunctionWrapper('switchToSelectedSession')
const getSessionInfo = createFunctionWrapper('getSessionInfo')
const listAllSessions = createFunctionWrapper('listAllSessions')
const deleteCurrentSession = createFunctionWrapper('deleteCurrentSession')
const updateSidebarFilesList = createFunctionWrapper('updateSidebarFilesList')
const handleKeyDown = createFunctionWrapper('handleKeyDown')
const getStreamData = createFunctionWrapper('getStreamData')
const clearStreamData = createFunctionWrapper('clearStreamData')
const exportChatHistory = createFunctionWrapper('exportChatHistory')
const quickDownloadNetlist = createFunctionWrapper('quickDownloadNetlist')
const listSessionFiles = createFunctionWrapper('listSessionFiles')
const getSessionTasks = createFunctionWrapper('getSessionTasks')
const getSessionStatus = createFunctionWrapper('getSessionStatus')
const getSessionOutput = createFunctionWrapper('getSessionOutput')
const clearOldFiles = createFunctionWrapper('clearOldFiles')
</script>

<style scoped>
/* Vue组件特定的样式调整 */
.llm-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}
</style>

<style>
/* 引入作用域化的LLM样式 - 使用相对路径 */
@import url('../../assets/llm-scoped-styles.css');
</style>