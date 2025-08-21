<script setup lang="ts">
import { ElInput, ElButton, ElMessageBox, ElMessage, ElIcon, ElSelect, ElOption } from 'element-plus'
import { ArrowUp, View, Download, Delete, MoreFilled, Setting, FolderOpened } from '@element-plus/icons-vue'
import { ref, watch } from 'vue'
import FileUpload from '../../components/FileUpload.vue'
import LLMConfig from '../../components/LLMConfig.vue'
import type { FileVO } from '../../api/file'
import { formatFileSize, downloadFile as apiDownloadFile, getFileList, deleteFile as apiDeleteFile } from '../../api/file'

// Agent类型定义
export type AgentType = 'orchestrator' | 'dynamic'

// 输入类型定义
export type InputType = 'question' | 'config' | 'intervention' | 'delete'

// 接收参数
const props = defineProps<{
  inputMessage: string
  isLoading: boolean
  inputDisabled: boolean
  isStreaming?: boolean
  uid: number
  sid: number
}>()

// 事件传递
const emit = defineEmits<{
  'update:input-message': [value: string]
  'send-message': [message: string, agentType: AgentType, inputType: InputType, files?: FileVO[]]
  'open-file-preview': [file: FileVO] // 新增：文件预览事件
  'toggle-file-preview': [] // 新增：切换文件预览窗口事件
  'create-session': [] // 新增：创建会话事件
}>()

// 响应式数据
const uploadedFiles = ref<FileVO[]>([])
const fileUploadRef = ref<InstanceType<typeof FileUpload>>()
const selectedAgentType = ref<AgentType>('orchestrator') // 默认使用orchestrator
const hasConfigSent = ref<Map<number, boolean>>(new Map()) // 跟踪每个会话是否已发送配置

// LLM配置相关
const showLLMConfig = ref(false)

// 定义LLM配置数据类型
interface LLMConfigData {
  apiKey: string
  baseUrl: string
  model: string
}

// Agent类型选项
const agentOptions = [
  { label: '编排代理 (Orchestrator)', value: 'orchestrator' as AgentType },
  { label: '动态代理 (Dynamic)', value: 'dynamic' as AgentType }
]

// 监听会话ID变化，加载对应的文件列表
watch(() => props.sid, async (newSid, oldSid) => {
  if (newSid !== oldSid && newSid > 0) {
    await loadSessionFiles()
    // 重置当前会话的配置发送状态
    if (!hasConfigSent.value.has(newSid)) {
      hasConfigSent.value.set(newSid, false)
    }
  }
}, { immediate: true })

// 加载当前会话的文件列表
const loadSessionFiles = async () => {
  if (!props.uid || !props.sid) {
    uploadedFiles.value = []
    return
  }
  
  try {
    console.log('加载会话文件列表:', { uid: props.uid, sid: props.sid })
    const response = await getFileList({ uid: props.uid, sid: props.sid })
    
    if (response.data && response.data.code === '200') {
      uploadedFiles.value = response.data.data.files || []
      console.log('文件列表加载成功:', uploadedFiles.value)
    } else {
      console.log('文件列表为空或加载失败')
      uploadedFiles.value = []
    }
  } catch (error) {
    console.error('加载文件列表失败:', error)
    uploadedFiles.value = []
  }
}

// 发送消息（按回车）
const handleKeyup = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 发送消息（点击按钮）
const sendMessage = async () => {
  if (props.inputMessage.trim() && !props.inputDisabled) {
    const sessionId = props.sid
    const userMessage = props.inputMessage.trim()
    
    // 检查是否需要先发送配置（隐式发送，用户不可见）
    if (!hasConfigSent.value.get(sessionId)) {
      console.log('首次发送问题，先隐式发送配置')
    
      // 隐式发送配置信息，不显示给用户
      emit('send-message', '', selectedAgentType.value, 'config' as InputType, undefined)
      
      // 标记配置已发送
      hasConfigSent.value.set(sessionId, true)
      
      // 等待一小段时间确保配置处理完成
      await new Promise(resolve => setTimeout(resolve, 500))
    }
    
    // 发送用户的实际问题（总是使用 question 类型）
    emit('send-message', userMessage, selectedAgentType.value, 'question' as InputType, uploadedFiles.value.length > 0 ? uploadedFiles.value : undefined)
    // 注意：不再清空文件列表，交由用户手动管理
  }
}

// 处理文件上传变化
const handleFilesChange = (files: FileVO[]) => {
  uploadedFiles.value = files
}

// 处理文件上传成功
const handleUploadSuccess = (file: FileVO) => {
  console.log('文件上传成功:', file)
  // 重新加载文件列表确保同步
  loadSessionFiles()
}

// 处理文件上传错误
const handleUploadError = (error: string) => {
  console.error('文件上传失败:', error)
}

// 处理文件预览
const handleFilePreview = (file: FileVO) => {
  console.log('🎯 ChatInput: handleFilePreview 函数被调用', {
    file,
    fileId: file.fileId,
    fileName: file.originalName
  })
  
  // 向父组件发送文件预览事件
  emit('open-file-preview', file)
}

// 处理创建会话
const handleCreateSession = () => {
  console.log('ChatInput: 处理创建会话事件')
  emit('create-session')
}

// 删除单个文件
const removeFile = async (file: FileVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.originalName}" 吗？`,
      '删除文件',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用后端API删除文件
    await apiDeleteFile(file.fileId)
    
    // 重新加载文件列表
    await loadSessionFiles()
    
    ElMessage.success('文件删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('文件删除失败')
      console.error('删除文件错误:', error)
    }
  }
}

// 获取文件扩展名
const getFileExtension = (filename: string) => {
  const lastDot = filename.lastIndexOf('.')
  return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : 'file'
}

// 下载文件
const downloadFile = async (file: FileVO) => {
  try {
    console.log('开始下载文件:', file.fileId)
    
    // 通过后端代理下载文件
    await apiDownloadFile(file.fileId, file.originalName)
    
    ElMessage.success('文件下载已开始')
  } catch (error) {
    console.error('文件下载失败:', error)
    ElMessage.error('文件下载失败')
  }
}

// LLM配置相关方法
const openLLMConfig = () => {
  showLLMConfig.value = true
}

// 文件预览相关方法
const openFilePreview = () => {
  // 发出事件给父组件，让父组件处理文件预览逻辑
  emit('toggle-file-preview')
}

const handleConfigSaved = async (configData: LLMConfigData | null) => {
  try {
    console.log('保存LLM配置:', configData)
    
    if (configData) {
      // 自定义模式：将配置数据序列化为JSON字符串传递
      const configMessage = JSON.stringify({
        apiKey: configData.apiKey,
        baseUrl: configData.baseUrl,
        model: configData.model
      })
      emit('send-message', configMessage, selectedAgentType.value, 'config' as InputType, undefined)
    } else {
      // 默认模式：发送空配置
      emit('send-message', "", selectedAgentType.value, 'config' as InputType, undefined)
    }
    
    // 标记配置已发送
    hasConfigSent.value.set(props.sid, true)
    
    ElMessage.success('LLM配置已保存')
    
  } catch (error) {
    console.error('配置保存失败:', error)
    ElMessage.error('配置保存失败')
  }
}
</script>

<template>
  <div class="chat-input">
    <!-- 文件列表显示区域 - 移到输入框上方 -->
    <div v-if="uploadedFiles.length > 0" class="files-attachment-area">
      <div class="files-carousel">
        <div 
          v-for="file in uploadedFiles" 
          :key="file.fileId"
          class="file-attachment-item"
          :title="file.originalName"
        >
          <div class="file-icon-wrapper">
            <span class="file-icon">📄</span>
          </div>
          <div class="file-content">
            <div class="file-name">{{ file.originalName }}</div>
            <div class="file-meta">
              <span class="file-type">{{ getFileExtension(file.originalName) }}</span>
              <span class="separator">·</span>
              <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
            </div>
          </div>
          
          <!-- 更多操作提示图标 -->
          <div class="more-actions-hint">
            <el-icon class="more-icon">
              <MoreFilled />
            </el-icon>
          </div>
          
          <div class="file-actions-menu">
            <el-button 
              size="small"
              link
              @click="handleFilePreview(file)"
              title="预览"
              class="action-btn"
            >
              <el-icon><View /></el-icon>
            </el-button>
            
            <el-button 
              size="small"
              link
              @click="downloadFile(file)"
              title="下载"
              class="action-btn"
            >
              <el-icon><Download /></el-icon>
            </el-button>
            <el-button 
              size="small"
              link
              @click="removeFile(file)"
              title="删除"
              class="action-btn delete-btn"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入框和发送按钮行 -->
    <div class="input-row">
      <el-input 
        :model-value="inputMessage"
        type="textarea" 
        :rows="1" 
        :autosize="{ minRows: 1, maxRows: 4 }"
        resize="none" 
        placeholder="输入您的问题..."
        @keyup="handleKeyup"
        :disabled="inputDisabled"
        @update:model-value="(val: string) => emit('update:input-message', val)"
        class="message-input"
      />

      <!-- 发送按钮 -->
      <el-button 
        type="primary" 
        @click="sendMessage" 
        :loading="isLoading || isStreaming"
        :disabled="inputDisabled || !inputMessage.trim()"
        :icon="ArrowUp"
        class="send-button"
        style="background-color: rgb(102, 8, 116); border-color: rgb(102, 8, 116);"
        title="发送消息"
        round
      >
        {{ isStreaming ? '生成中...' : '发送' }}
      </el-button>
    </div>

    <!-- 控制按钮行 -->
    <div class="control-row">
      <!-- Agent类型选择器 -->
      <el-select
        v-model="selectedAgentType"
        placeholder="选择代理"
        class="agent-selector"
        size="small"
        :disabled="inputDisabled"
      >
        <el-option
          v-for="option in agentOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      
      <!-- 文件上传组件，直接嵌入到按钮位置 -->
      <FileUpload
        ref="fileUploadRef"
        :uid="props.uid"
        :sid="props.sid"
        :max-size="100"
        @files-change="handleFilesChange"
        @upload-success="handleUploadSuccess"
        @upload-error="handleUploadError"
        @create-session="handleCreateSession"
        @file-preview="handleFilePreview"
      />

      <!-- 文件预览按钮 -->
      <el-button 
        type="default"
        @click="openFilePreview"
        :disabled="false"
        :icon="FolderOpened"
        class="control-button"
        title="文件预览"
        circle
      />

      <!-- LLM配置按钮 -->
      <el-button 
        type="default"
        @click="openLLMConfig"
        :disabled="inputDisabled"
        :icon="Setting"
        class="control-button"
        title="LLM配置"
        circle
      />
    </div>

    <!-- 底部提示 -->
    <div class="input-footer">
      <div class="input-tips">
        <span>按 Enter 发送，Shift + Enter 换行</span>
        <span v-if="uploadedFiles.length > 0" class="file-count">
          · 已选择 {{ uploadedFiles.length }} 个文件
        </span>
        <span class="agent-hint">
          · {{ agentOptions.find(opt => opt.value === selectedAgentType)?.label }}
        </span>
      </div>
    </div>

    <!-- LLM配置对话框 -->
    <LLMConfig
      v-model:visible="showLLMConfig"
      :uid="props.uid"
      :sid="props.sid"
      @config-saved="handleConfigSaved"
    />
  </div>
</template>

<style scoped>
.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #e0e0e0;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  box-sizing: border-box;
}

/* 文件附件区域 */
.files-attachment-area {
  background: white;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
}

.files-carousel {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.file-attachment-item {
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 8px 12px;
  min-width: 200px;
  max-width: 300px;
  border: 1px solid #e4e7ed;
  position: relative;
  transition: all 0.2s ease;
}

.file-attachment-item:hover {
  border-color: rgb(102, 8, 116);
  box-shadow: 0 2px 8px rgba(102, 8, 116, 0.1);
}

.file-icon-wrapper {
  margin-right: 8px;
  flex-shrink: 0;
}

.file-icon {
  font-size: 24px;
}

.file-content {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 12px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.file-meta {
  font-size: 10px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.file-type {
  text-transform: uppercase;
}

.separator {
  margin: 0 2px;
}

.more-actions-hint {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0.6;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.more-icon {
  font-size: 16px;
  color: #909399;
}

.file-attachment-item:hover .more-actions-hint {
  opacity: 0;
}

.file-actions-menu {
  display: none;
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  gap: 4px;
}

.file-attachment-item:hover .file-actions-menu {
  display: flex;
}

.action-btn {
  width: 28px !important;
  height: 28px !important;
  min-width: 28px !important;
  border-radius: 6px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: #666 !important;
  transition: all 0.2s ease !important;
  border: 1px solid transparent !important;
  padding: 0 !important;
}

.action-btn:hover {
  background-color: #f5f5f5 !important;
  color: #333 !important;
  border-color: #e0e0e0 !important;
  transform: scale(1.05);
}

.delete-btn:hover {
  background-color: #fef2f2 !important;
  color: #ef4444 !important;
  border-color: #fecaca !important;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 8px;
}

.control-row {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-start;
  padding-top: 4px;
}

.message-input {
  flex: 1;
}

.agent-selector {
  flex-shrink: 0;
  width: 140px;
}

.send-button {
  flex-shrink: 0;
  height: 40px;
  min-width: 80px;
}

.control-button {
  flex-shrink: 0;
  height: 32px;
  width: 32px;
  border-color: #dcdfe6;
  color: #606266;
  transition: all 0.2s ease;
}

.control-button:hover {
  border-color: rgb(102, 8, 116);
  color: rgb(102, 8, 116);
}

.input-footer {
  display: flex;
  justify-content: center;
}

.input-tips {
  font-size: 0.7em;
  color: #999;
  text-align: center;
  margin: 0;
  padding: 0;
}

.file-count {
  color: rgb(102, 8, 116);
  font-weight: 500;
}

.agent-hint {
  color: #666;
  font-weight: 400;
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
  border-color: #dcdfe6;
  padding: 10px 12px;
  line-height: 1.4;
  min-height: 40px;
}

:deep(.el-textarea__inner):focus {
  border-color: rgb(102, 8, 116);
}

/* Agent选择器样式 */
:deep(.agent-selector .el-select__wrapper) {
  border-radius: 8px;
  border-color: #dcdfe6;
  height: 32px;
}

:deep(.agent-selector .el-select__wrapper.is-focused) {
  border-color: rgb(102, 8, 116);
}

:deep(.agent-selector .el-select__placeholder) {
  font-size: 12px;
  color: #a8abb2;
}

/* 文件预览 Popover 样式 */
:deep(.file-preview-popover) {
  --el-popover-padding: 0;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  max-height: 600px;
  overflow: hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .input-row {
    gap: 6px;
  }
  
  .control-row {
    flex-wrap: wrap;
    gap: 6px;
  }
  
  .agent-selector {
    width: 120px;
  }
  
  .files-carousel {
    flex-direction: column;
  }
  
  .file-attachment-item {
    min-width: auto;
    max-width: none;
  }
}
</style>
