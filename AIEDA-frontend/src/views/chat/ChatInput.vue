<script setup lang="ts">
import { ElInput, ElButton, ElMessageBox, ElMessage, ElIcon } from 'element-plus'
import { ArrowUp, View, Download, Delete, MoreFilled } from '@element-plus/icons-vue'
import { ref, watch } from 'vue'
import FileUpload from '../../components/FileUpload.vue'
import type { FileVO } from '../../api/file'
import { formatFileSize, downloadFile as apiDownloadFile, getFileList, deleteFile as apiDeleteFile } from '../../api/file'

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
  'send-message': [message: string, files?: FileVO[]]
}>()

// 响应式数据
const uploadedFiles = ref<FileVO[]>([])
const fileUploadRef = ref<InstanceType<typeof FileUpload>>()

// 监听会话ID变化，加载对应的文件列表
watch(() => props.sid, async (newSid, oldSid) => {
  if (newSid !== oldSid && newSid > 0) {
    await loadSessionFiles()
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
const sendMessage = () => {
  if (props.inputMessage.trim() && !props.inputDisabled) {
    emit('send-message', props.inputMessage.trim(), uploadedFiles.value.length > 0 ? uploadedFiles.value : undefined)
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
  console.log('预览文件:', file)
  // 这里可以打开文件预览组件或新窗口
  // 可以触发一个事件让父组件处理
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
    const blob = await apiDownloadFile(file.fileId)
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.originalName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('文件下载成功')
  } catch (error) {
    console.error('文件下载失败:', error)
    ElMessage.error('文件下载失败')
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
              text
              @click="handleFilePreview(file)"
              title="预览"
              class="action-btn"
            >
              <el-icon><View /></el-icon>
            </el-button>
            <el-button 
              size="small"
              text
              @click="downloadFile(file)"
              title="下载"
              class="action-btn"
            >
              <el-icon><Download /></el-icon>
            </el-button>
            <el-button 
              size="small"
              text
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
      
      <!-- 文件上传组件，直接嵌入到按钮位置 -->
      <FileUpload
        ref="fileUploadRef"
        :uid="props.uid"
        :sid="props.sid"
        :max-files="5"
        :max-size="50"
        :accept-types="[
          'image/', 'text/', 
          'pdf', 'txt', 'md', 'json', 'xml', 'csv',
          'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
          '.doc', '.docx', '.pdf', '.txt', '.md', '.json', '.xml', '.csv'
        ]"
        @files-change="handleFilesChange"
        @upload-success="handleUploadSuccess"
        @upload-error="handleUploadError"
        @file-preview="handleFilePreview"
      />

      <!-- 临时调试按钮 -->
      <!-- <el-button 
        type="warning"
        @click="debugShowUpload"
        size="small"
        style="margin-left: 4px;"
      >
        DEBUG
      </el-button> -->

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

    <!-- 底部提示 -->
    <div class="input-footer">
      <div class="input-tips">
        <span>按 Enter 发送，Shift + Enter 换行</span>
        <span v-if="uploadedFiles.length > 0" class="file-count">
          · 已选择 {{ uploadedFiles.length }} 个文件
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #e0e0e0;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
  gap: 6px;
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
}

.message-input {
  flex: 1;
}

.file-button {
  flex-shrink: 0;
  height: 40px;
  width: 40px;
}

.send-button {
  flex-shrink: 0;
  height: 40px;
  min-width: 80px;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .files-carousel {
    flex-direction: column;
  }
  
  .file-attachment-item {
    min-width: auto;
    max-width: none;
  }
}
</style>
