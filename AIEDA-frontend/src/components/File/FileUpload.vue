<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElUpload, ElButton, ElIcon, ElMessage, ElPopover } from 'element-plus'
import { Upload, Close, Paperclip } from '@element-plus/icons-vue'
import { uploadFile, type FileVO, canPreviewFile } from '../../api/file'

// 组件属性
const props = defineProps<{
  uid: number
  sid: number
  maxFiles?: number
  maxSize?: number // MB
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
const uploadMode = ref<'file' | 'folder'>('file') // 新增：上传模式选择
const folderInputRef = ref<HTMLInputElement>()

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
const maxFileSize = computed(() => (props.maxSize || 100) * 1024 * 1024) // 转换为字节，默认100MB

// 文件上传前的检查
const beforeUpload = (file: File) => {
  // 检查文件大小
  if (file.size > maxFileSize.value) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize || 100}MB`)
    return false
  }

  // 检查文件类型（黑名单模式）
  if (!canPreviewFile(file.type, file.name)) {
    const fileExtension = file.name.substring(file.name.lastIndexOf('.'))
    ElMessage.error(`不支持上传此文件类型: ${fileExtension || file.type}`)
    return false
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

// 触发文件夹上传
const triggerFolderUpload = () => {
  if (folderInputRef.value) {
    folderInputRef.value.click()
  }
}

// 处理文件夹上传
const handleFolderUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = input.files
  
  if (!files || files.length === 0) {
    return
  }

  isUploading.value = true

  try {
    let sessionId = props.sid
    
    // 如果没有会话ID（sid为0），先创建会话
    if (sessionId === 0) {
      console.log('没有会话ID，自动创建会话并继续上传')
      emit('create-session')
      sessionId = await waitForSession()
      console.log('会话创建完成，ID:', sessionId)
    }
    
    // 按文件夹结构上传文件
    await uploadFolderFiles(Array.from(files), sessionId)
  } catch (error: any) {
    console.error('文件夹上传错误:', error)
    const errorMessage = error.message || '文件夹上传失败'
    emit('upload-error', errorMessage)
    ElMessage.error(errorMessage)
  } finally {
    isUploading.value = false
    // 清空文件输入
    input.value = ''
  }
}

// 处理文件夹拖拽上传
const handleFolderDrop = async (event: DragEvent) => {
  event.preventDefault()
  
  const items = event.dataTransfer?.items
  if (!items) return

  const files: File[] = []
  
  // 遍历拖拽的项目，提取文件
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item.kind === 'file') {
      const entry = item.webkitGetAsEntry()
      if (entry) {
        await traverseFileTree(entry, '', files)
      }
    }
  }

  if (files.length === 0) {
    ElMessage.warning('未检测到有效的文件')
    return
  }

  isUploading.value = true

  try {
    let sessionId = props.sid
    
    if (sessionId === 0) {
      emit('create-session')
      sessionId = await waitForSession()
    }
    
    await uploadFolderFiles(files, sessionId)
  } catch (error: any) {
    console.error('文件夹拖拽上传错误:', error)
    const errorMessage = error.message || '文件夹上传失败'
    emit('upload-error', errorMessage)
    ElMessage.error(errorMessage)
  } finally {
    isUploading.value = false
  }
}

// 遍历文件树（用于拖拽上传）
const traverseFileTree = async (item: any, path: string, files: File[]): Promise<void> => {
  return new Promise((resolve) => {
    if (item.isFile) {
      item.file((file: File) => {
        // 设置文件的相对路径
        const relativePath = path ? `${path}/${file.name}` : file.name
        Object.defineProperty(file, 'webkitRelativePath', {
          value: relativePath,
          writable: false
        })
        files.push(file)
        resolve()
      })
    } else if (item.isDirectory) {
      const dirReader = item.createReader()
      dirReader.readEntries(async (entries: any[]) => {
        const promises = entries.map(entry => 
          traverseFileTree(entry, path ? `${path}/${item.name}` : item.name, files)
        )
        await Promise.all(promises)
        resolve()
      })
    }
  })
}

// 上传文件夹中的文件
const uploadFolderFiles = async (files: File[], sessionId: number) => {
  console.log('开始上传文件夹，文件数量:', files.length)
  
  for (const file of files) {
    // 检查单个文件
    if (!beforeUpload(file)) {
      continue
    }
    
    try {
      // 获取文件的相对路径
      const relativePath = file.webkitRelativePath || file.name
      console.log('上传文件:', relativePath)
      
      // 修改文件上传，包含文件夹结构信息
      await performFolderFileUpload(file, sessionId, relativePath)
    } catch (error) {
      console.error(`上传文件 ${file.name} 失败:`, error)
      ElMessage.error(`上传文件 ${file.name} 失败`)
    }
  }
  
  ElMessage.success('文件夹上传完成')
  hideUpload()
}

// 上传单个文件夹文件
const performFolderFileUpload = async (file: File, sessionId: number, relativePath: string) => {
  console.log('开始上传文件夹文件:', file.name, '路径:', relativePath)
  
  const response = await uploadFile({
    uid: props.uid,
    sid: sessionId,
    file: file,
    metadata: JSON.stringify({
      originalName: file.name,
      fileSize: file.size,
      fileType: file.type,
      folderPath: relativePath // 添加文件夹路径信息
    })
  })

  console.log('文件夹文件上传响应:', response)
  
  if (response.data && response.data.data) {
    const fileVO: FileVO = response.data.data
    uploadedFiles.value.push(fileVO)
    
    emit('files-change', [...uploadedFiles.value])
    emit('upload-success', fileVO)
    
    console.log(`文件 "${relativePath}" 上传成功`)
  } else {
    console.error('响应格式错误，缺少 data 字段:', response)
    throw new Error('响应格式错误')
  }
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
      v-model:visible="showUploadForm"
      placement="top"
      :width="400"
      popper-class="file-upload-popover"
      trigger="click"
      :hide-after="0"
    >
      <template #reference>
        <el-button 
          type="primary" 
          :icon="Paperclip" 
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
          
          <!-- 上传模式选择器 -->
          <div class="upload-mode-selector">
            <el-button 
              :type="uploadMode === 'file' ? 'primary' : 'default'"
              @click="uploadMode = 'file'"
              size="small"
              style="background-color: rgb(102, 8, 116); border-color: rgb(102, 8, 116);"
              :class="{ 'active-mode': uploadMode === 'file' }"
            >
              上传文件
            </el-button>
            <el-button 
              :type="uploadMode === 'folder' ? 'primary' : 'default'"
              @click="uploadMode = 'folder'"
              size="small"
              style="background-color: rgb(102, 8, 116); border-color: rgb(102, 8, 116);"
              :class="{ 'active-mode': uploadMode === 'folder' }"
            >
              上传文件夹
            </el-button>
          </div>
          
          <div class="upload-area">
            <!-- 文件上传区域 -->
            <el-upload
              v-if="uploadMode === 'file'"
              :http-request="customUpload"
              :show-file-list="false"
              :disabled="isUploading"
              drag
              multiple
              class="upload-dragger"
            >
              <el-icon class="upload-icon"><upload /></el-icon>
              <div class="upload-text">
                <div class="upload-primary">点击或拖拽文件到此区域上传</div>
                <div class="upload-hint">
                  <span>单个文件不超过 {{ props.maxSize || 100 }}MB</span>
                  <span>，不支持 zip 等压缩文件</span>
                </div>
              </div>
            </el-upload>
            
            <!-- 文件夹上传区域 -->
            <div v-else-if="uploadMode === 'folder'" class="folder-upload-area">
              <input
                ref="folderInputRef"
                type="file"
                webkitdirectory
                directory
                multiple
                @change="handleFolderUpload"
                style="display: none;"
              />
              <div 
                class="folder-upload-dragger"
                @click="triggerFolderUpload"
                @drop="handleFolderDrop"
                @dragover.prevent
                @dragenter.prevent
              >
                <el-icon class="upload-icon"><upload /></el-icon>
                <div class="upload-text">
                  <div class="upload-primary">点击选择文件夹上传</div>
                  <div class="upload-hint">
                    <span>将保持文件夹结构</span>
                    <span>，单个文件不超过 {{ props.maxSize || 100 }}MB</span>
                  </div>
                </div>
              </div>
            </div>
            
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

/* 上传模式选择器样式 */
.upload-mode-selector {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 16px;
  padding: 8px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.upload-mode-selector .el-button {
  flex: 1;
  max-width: 120px;
}

.upload-mode-selector .el-button.active-mode {
  background-color: rgb(102, 8, 116) !important;
  border-color: rgb(102, 8, 116) !important;
  color: white !important;
}

.upload-mode-selector .el-button:not(.active-mode) {
  background-color: white !important;
  border-color: #dcdfe6 !important;
  color: #606266 !important;
}

.upload-mode-selector .el-button:not(.active-mode):hover {
  border-color: rgb(102, 8, 116) !important;
  color: rgb(102, 8, 116) !important;
}

/* 文件夹上传区域样式 */
.folder-upload-area {
  width: 100%;
}

.folder-upload-dragger {
  width: 100%;
  height: 120px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  background-color: #fafafa;
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.folder-upload-dragger:hover {
  border-color: rgb(102, 8, 116);
  background-color: rgba(102, 8, 116, 0.02);
}

.folder-upload-dragger.is-dragover {
  border-color: rgb(102, 8, 116);
  background-color: rgba(102, 8, 116, 0.05);
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
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
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
