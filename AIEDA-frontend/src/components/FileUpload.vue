<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElUpload, ElButton, ElIcon, ElMessage, ElPopover } from 'element-plus'
import { Upload, Close, Paperclip } from '@element-plus/icons-vue'
import { uploadFile, type FileVO } from '../api/file'

// 组件属性
const props = defineProps<{
  uid: number
  sid: number
  maxFiles?: number
  maxSize?: number // MB
  acceptTypes?: string[]
}>()

// 组件事件
const emit = defineEmits<{
  'files-change': [files: FileVO[]]
  'upload-success': [file: FileVO]
  'upload-error': [error: string]
  'file-preview': [file: FileVO]
  'create-session': [] // 新增：创建会话事件
}>()

// 响应式数据
const uploadedFiles = ref<FileVO[]>([])
const isUploading = ref(false)
const showUploadForm = ref(false)

// 调试：组件挂载时的日志
onMounted(() => {
  console.log('FileUpload 组件已挂载，props:', props)
  console.log('初始文件列表长度:', uploadedFiles.value.length)
  console.log('初始 showUploadForm 值:', showUploadForm.value)
})

// 调试：监听文件列表变化
watch(uploadedFiles, (newFiles) => {
  console.log('文件列表发生变化:', newFiles)
  console.log('新的文件列表长度:', newFiles.length)
}, { deep: true })

// 调试：监听上传表单显示状态变化
watch(showUploadForm, (newValue) => {
  console.log('showUploadForm 状态变化:', newValue)
})

// 计算属性
const acceptTypesString = computed(() => {
  if (!props.acceptTypes || props.acceptTypes.length === 0) {
    return undefined
  }
  return props.acceptTypes.join(',')
})

const maxFileSize = computed(() => (props.maxSize || 10) * 1024 * 1024) // 转换为字节

// 文件上传前的检查
const beforeUpload = (file: File) => {
  // 检查文件数量
  if (props.maxFiles && uploadedFiles.value.length >= props.maxFiles) {
    ElMessage.error(`最多只能上传 ${props.maxFiles} 个文件`)
    return false
  }

  // 检查文件大小
  if (file.size > maxFileSize.value) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize || 10}MB`)
    return false
  }

  // 检查文件类型
  if (props.acceptTypes && props.acceptTypes.length > 0) {
    const fileName = file.name.toLowerCase()
    const fileType = file.type.toLowerCase()
    
    const isValidType = props.acceptTypes.some(type => {
      const lowerType = type.toLowerCase()
      
      // 处理扩展名（以.开头）
      if (lowerType.startsWith('.')) {
        return fileName.endsWith(lowerType)
      }
      
      // 处理MIME类型（如 image/、text/）
      if (lowerType.includes('/')) {
        return fileType.startsWith(lowerType)
      }
      
      // 处理特定文档类型的MIME映射
      const documentMimeTypes: Record<string, string[]> = {
        'pdf': ['application/pdf'],
        'txt': ['text/plain', 'text/txt'],
        'md': ['text/markdown', 'text/x-markdown'],
        'doc': ['application/msword'],
        'docx': ['application/vnd.openxmlformats-officedocument.wordprocessingml.document'],
        'xls': ['application/vnd.ms-excel'],
        'xlsx': ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
        'ppt': ['application/vnd.ms-powerpoint'],
        'pptx': ['application/vnd.openxmlformats-officedocument.presentationml.presentation'],
        'json': ['application/json', 'text/json'],
        'xml': ['application/xml', 'text/xml'],
        'csv': ['text/csv', 'application/csv'],
        'rtf': ['application/rtf', 'text/rtf']
      }
      
      // 根据扩展名检查
      if (documentMimeTypes[lowerType]) {
        return documentMimeTypes[lowerType].includes(fileType) || fileName.endsWith(`.${lowerType}`)
      }
      
      // 其他情况：检查文件扩展名
      return fileName.endsWith(`.${lowerType}`)
    })
    
    if (!isValidType) {
      const fileExtension = fileName.substring(fileName.lastIndexOf('.'))
      ElMessage.error(`不支持的文件类型: ${fileExtension || file.type}，支持的格式: ${props.acceptTypes.join(', ')}`)
      return false
    }
  }

  return true
}

// 执行实际的文件上传
const performUpload = async (file: File, sessionId: number) => {
  console.log('开始上传文件:', file.name, '会话ID:', sessionId)
  
  const response = await uploadFile({
    uid: props.uid,
    sid: sessionId,
    file: file,
    metadata: JSON.stringify({
      originalName: file.name,
      fileSize: file.size,
      fileType: file.type
    })
  })

  console.log('上传响应:', response)
  
  if (response.data && response.data.data) {
    console.log('上传成功，文件数据:', response.data.data)
    const fileVO: FileVO = response.data.data
    uploadedFiles.value.push(fileVO)
    
    emit('files-change', [...uploadedFiles.value])
    emit('upload-success', fileVO)
    
    ElMessage.success(`文件 "${file.name}" 上传成功`)
    console.log('上传成功消息已显示')
    
    // 自动关闭上传表单
    hideUpload()
  } else {
    console.error('响应格式错误，缺少 data 字段:', response)
    throw new Error('响应格式错误')
  }
}

// 等待会话创建完成的函数
const waitForSession = (): Promise<number> => {
  return new Promise((resolve, reject) => {
    const checkSession = () => {
      if (props.sid !== 0) {
        resolve(props.sid)
      } else {
        // 每100ms检查一次会话是否创建完成，最多等待10秒
        setTimeout(checkSession, 100)
      }
    }
    
    // 设置10秒超时
    setTimeout(() => {
      reject(new Error('创建会话超时'))
    }, 10000)
    
    checkSession()
  })
}

// 自定义上传函数
const customUpload = async (options: any) => {
  const { file } = options
  
  if (!beforeUpload(file)) {
    return
  }

  isUploading.value = true

  try {
    let sessionId = props.sid
    
    // 如果没有会话ID（sid为0），先创建会话
    if (sessionId === 0) {
      console.log('没有会话ID，自动创建会话并继续上传')
      
      // 触发创建会话事件
      emit('create-session')
      
      // 等待会话创建完成
      sessionId = await waitForSession()
      console.log('会话创建完成，ID:', sessionId)
    }
    
    // 执行文件上传
    await performUpload(file, sessionId)
  } catch (error: any) {
    console.error('文件上传错误详情:', error)
    const errorMessage = error.message || '文件上传失败'
    emit('upload-error', errorMessage)
    ElMessage.error(errorMessage)
  } finally {
    isUploading.value = false
  }
}

// 清空所有文件
const clearAllFiles = () => {
  uploadedFiles.value = []
  emit('files-change', [])
}

// 显示上传表单
const showUpload = () => {
  console.log('FileUpload: showUpload 方法被调用')
  console.log('FileUpload: 当前 showUploadForm 值:', showUploadForm.value)
  showUploadForm.value = true
  console.log('FileUpload: 设置后 showUploadForm 值:', showUploadForm.value)
}

// 隐藏上传表单
const hideUpload = () => {
  console.log('FileUpload: hideUpload 方法被调用')
  showUploadForm.value = false
}

// 切换上传表单显示状态
const toggleUpload = () => {
  console.log('FileUpload: toggleUpload 方法被调用，当前状态:', showUploadForm.value)
  showUploadForm.value = !showUploadForm.value
  console.log('FileUpload: 切换后状态:', showUploadForm.value)
}

// 获取上传的文件列表
const getUploadedFiles = () => {
  return [...uploadedFiles.value]
}

// 暴露方法给父组件
defineExpose({
  clearAllFiles,
  getUploadedFiles,
  showUpload,
  hideUpload,
  toggleUpload
})
</script>

<template>
  <div class="file-upload-component">
    <!-- 使用 Popover 的上传按钮 -->
    <el-popover
      :visible="showUploadForm"
      placement="top"
      :width="400"
      popper-class="file-upload-popover"
    >
      <template #reference>
        <el-button 
          type="primary" 
          :icon="Paperclip" 
          @click="toggleUpload"
          class="file-button"
          title="上传文件"
          style="background-color: rgb(102, 8, 116); border-color: rgb(102, 8, 116); height: 32px; width: 32px;"
          circle
        />
      </template>
      
      <!-- Popover 内容使用默认插槽 -->
      <template #default>
        <div class="upload-content">
          <div class="upload-header">
            <h3>上传文件</h3>
            <el-button 
              type="text" 
              :icon="Close" 
              @click="hideUpload"
              class="close-btn"
              size="small"
            />
          </div>
          
          <div class="upload-area">
            <el-upload
              :http-request="customUpload"
              :show-file-list="false"
              :accept="acceptTypesString"
              :disabled="isUploading"
              drag
              multiple
              class="upload-dragger"
            >
              <el-icon class="upload-icon"><upload /></el-icon>
              <div class="upload-text">
                <div class="upload-primary">点击或拖拽文件到此区域上传</div>
                <div class="upload-hint">
                  <span v-if="props.maxFiles">最多上传 {{ props.maxFiles }} 个文件，</span>
                  <span>单个文件不超过 {{ props.maxSize || 10 }}MB</span>
                  <span v-if="props.acceptTypes && props.acceptTypes.length > 0">
                    ，支持格式: {{ props.acceptTypes.join(', ') }}
                  </span>
                </div>
              </div>
            </el-upload>
            
            <div class="upload-status" v-if="isUploading">
              <span>正在上传...</span>
            </div>
          </div>
        </div>
      </template>
    </el-popover>
  </div>
</template>

<style scoped>
.file-upload-component {
  display: flex;
  align-items: flex-end;
  flex-shrink: 0;
}

.file-button {
  flex-shrink: 0;
}

/* Popover 内容样式 */
:deep(.file-upload-popover) {
  --el-popover-padding: 0;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.upload-content {
  padding: 20px;
}

.upload-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.upload-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.close-btn {
  padding: 4px;
  color: #909399;
}

.close-btn:hover {
  color: #606266;
}

.upload-area {
  text-align: center;
}

.upload-dragger {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 120px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  background-color: #fafafa;
  transition: all 0.3s ease;
}

:deep(.el-upload-dragger:hover) {
  border-color: rgb(102, 8, 116);
  background-color: rgba(102, 8, 116, 0.02);
}

:deep(.el-upload-dragger.is-dragover) {
  border-color: rgb(102, 8, 116);
  background-color: rgba(102, 8, 116, 0.05);
}

.upload-icon {
  font-size: 32px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.upload-text {
  color: #606266;
  line-height: 1.6;
}

.upload-primary {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
}

.upload-status {
  margin-top: 12px;
  padding: 8px 16px;
  background-color: rgba(102, 8, 116, 0.1);
  border-radius: 6px;
  font-size: 14px;
  color: rgb(102, 8, 116);
}

/* 已上传文件列表 */
.uploaded-files {
  margin-top: 16px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  background-color: #fafafa;
  overflow: hidden;
}

.files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.files-count {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.files-list {
  max-height: 200px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
  transition: background-color 0.2s ease;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item:hover {
  background-color: #ffffff;
}

.file-info {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.file-icon {
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .file-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .file-actions {
    margin-left: 0;
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
