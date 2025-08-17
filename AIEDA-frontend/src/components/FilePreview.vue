<template>
  <div v-if="visible" class="file-preview-overlay">
    <!-- 侧边面板模式 -->
    <div class="side-panel-container">
      <!-- 左侧文件目录 -->
      <div class="directory-panel" :class="{ 'collapsed': directoryCollapsed }">
        <div class="directory-header" :class="{ 'collapsed': directoryCollapsed }">
          <div class="header-title" v-if="!directoryCollapsed">
            <el-icon class="title-icon"><FolderOpened /></el-icon>
            <span class="title-text">文件目录</span>
          </div>
          <div class="header-actions" :class="{ 'vertical': directoryCollapsed }" v-if="!directoryCollapsed">
            <el-button 
              :icon="Refresh" 
              @click="loadFileList"
              :loading="isLoading"
              size="small"
              circle
              title="刷新文件列表"
            />
            <el-button 
              :icon="ArrowLeft" 
              @click="directoryCollapsed = !directoryCollapsed"
              size="small"
              circle
              title="收起目录"
            />
          </div>
          <!-- 收起状态下的菜单项 -->
          <div class="collapsed-menu" v-if="directoryCollapsed">
            <div 
              class="menu-item" 
              @click="loadFileList"
              :class="{ 'loading': isLoading }"
              title="刷新文件列表"
            >
              <el-icon><Refresh /></el-icon>
            </div>
            <div 
              class="menu-item" 
              @click="directoryCollapsed = !directoryCollapsed"
              title="展开目录"
            >
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
        
        <div v-if="!directoryCollapsed" class="directory-content">
          <div class="file-tree" v-loading="isLoading">
            <!-- 空状态 -->
            <div v-if="Object.keys(fileStructure).length === 0 && !isLoading" class="empty-state">
              <el-icon class="empty-icon"><Document /></el-icon>
              <p class="empty-text">当前会话暂无文件</p>
              <p class="empty-hint">上传文件后将在此处显示</p>
            </div>

            <!-- 层次化文件树 -->
            <FileTreeNodeRecursive 
              v-for="(nodeValue, nodeName) in fileStructure" 
              :key="`root-${nodeName}`"
              :node-name="String(nodeName)"
              :node-value="nodeValue"
              :selected-file-id="selectedFileId"
              @file-selected="handleFileSelected"
            />
          </div>
        </div>
      </div>
      
      <!-- 右侧文件预览 -->
      <div class="explorer-panel">
        <div class="explorer-header">
          <div class="header-left">
            <div class="file-icon">
              <el-icon size="24" color="#409eff">
                <Document />
              </el-icon>
            </div>
            <div class="file-info">
              <h3 class="file-name">{{ selectedFile?.originalName || '请选择文件' }}</h3>
              <div v-if="selectedFile" class="file-meta">
                <el-tag size="small" type="info">{{ getFileTypeDisplay(selectedFile?.fileType) }}</el-tag>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button 
              v-if="selectedFile"
              type="primary" 
              :icon="Download" 
              @click="downloadFile(selectedFile)"
              :loading="isDownloading"
              size="small"
            >
              下载
            </el-button>
            <el-button 
              type="default" 
              :icon="Close" 
              @click="handleClose"
              size="small"
              circle
              title="关闭"
            />
          </div>
        </div>
        
        <!-- 文件预览内容 -->
        <div class="preview-content" v-loading="isLoading">
          <template v-if="selectedFile">
            <!-- 图片预览 -->
            <template v-if="previewType === 'image' && previewUrl">
              <div class="image-preview">
                <img :src="previewUrl" :alt="selectedFile.originalName" class="preview-image" />
              </div>
            </template>
            
            <!-- PDF预览 -->
            <template v-else-if="previewType === 'pdf' && previewUrl">
              <div class="pdf-preview">
                <iframe :src="previewUrl" class="pdf-iframe" frameborder="0"></iframe>
              </div>
            </template>
            
            <!-- 文本预览 -->
            <template v-else-if="previewType === 'text' && previewContent">
              <div class="text-preview">
                <pre class="text-content">{{ previewContent }}</pre>
              </div>
            </template>
            
            <!-- 不支持预览的文件 -->
            <template v-else-if="previewType === 'unsupported'">
              <div class="no-preview">
                <el-icon class="no-preview-icon"><Document /></el-icon>
                <p class="no-preview-text">此文件类型不支持在线预览</p>
                <p class="no-preview-hint">请点击下载按钮下载文件后查看</p>
                <el-button 
                  type="primary" 
                  :icon="Download" 
                  @click="downloadFile(selectedFile)"
                  :loading="isDownloading"
                >
                  下载文件
                </el-button>
              </div>
            </template>
            
            <!-- 加载状态 -->
            <template v-else-if="isLoading">
              <div class="preview-placeholder">
                <el-icon class="placeholder-icon"><View /></el-icon>
                <p class="placeholder-text">正在加载预览...</p>
              </div>
            </template>
          </template>
          
          <!-- 未选择文件 -->
          <template v-else>
            <div class="preview-placeholder">
              <el-icon class="placeholder-icon"><View /></el-icon>
              <p class="placeholder-text">请选择要预览的文件</p>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElButton, ElIcon, ElTag, ElMessage } from 'element-plus'
import { 
  FolderOpened, 
  Refresh, 
  Close, 
  Document, 
  Download, 
  View, 
  ArrowLeft, 
  ArrowRight 
} from '@element-plus/icons-vue'
import { getHierarchicalFileStructure, downloadFile as apiDownloadFile, previewFile, canPreviewFile, type FileVO } from '../api/file'
import FileTreeNodeRecursive from './FileTreeNodeRecursive.vue'

// 组件属性
const props = defineProps<{
  uid: number
  sid: number
  visible: boolean
  selectedFileId?: string
}>()

// 组件事件
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'close': []
}>()

// 响应式数据
const fileStructure = ref<Record<string, any>>({}) // 直接存储LLM返回的原始结构
const selectedFile = ref<FileVO | null>(null)
const selectedFileId = ref<string>('')
const previewContent = ref('')
const previewUrl = ref('')
const previewType = ref<'text' | 'image' | 'pdf' | 'unsupported'>('unsupported')
const isLoading = ref(false)
const isDownloading = ref(false)
const directoryCollapsed = ref(false)

// 获取文件类型显示名称
const getFileTypeDisplay = (fileType: string): string => {
  if (fileType?.startsWith('text/')) return 'Text'
  if (fileType?.startsWith('image/')) return 'Image'
  if (fileType?.includes('pdf')) return 'PDF'
  if (fileType?.includes('json')) return 'JSON'
  if (fileType?.includes('xml')) return 'XML'
  if (fileType?.includes('csv')) return 'CSV'
  if (fileType?.includes('markdown')) return 'Markdown'
  if (fileType?.includes('javascript')) return 'JavaScript'
  if (fileType?.includes('word') || fileType?.includes('msword')) return 'Word'
  if (fileType?.includes('excel') || fileType?.includes('sheet')) return 'Excel'
  if (fileType?.includes('powerpoint') || fileType?.includes('presentation')) return 'PowerPoint'
  
  return 'File'
}

// 加载文件列表
const loadFileList = async () => {
  if (!props.uid || !props.sid) {
    fileStructure.value = {}
    return
  }

  try {
    isLoading.value = true
    console.log('加载层次化文件结构:', { uid: props.uid, sid: props.sid })
    
    const response = await getHierarchicalFileStructure({ uid: props.uid, sid: props.sid })
    
    if (response.data && response.data.code === '200') {
      fileStructure.value = response.data.data || {}
      console.log('层次化文件结构加载成功:', fileStructure.value)
      
      // 如果有指定的文件ID，自动选择该文件
      if (props.selectedFileId) {
        selectedFileId.value = props.selectedFileId
      }
    } else {
      console.log('层次化文件结构为空或加载失败')
      fileStructure.value = {}
    }
  } catch (error) {
    console.error('加载层次化文件结构失败:', error)
    fileStructure.value = {}
  } finally {
    isLoading.value = false
  }
}

// 选择文件进行预览
const selectFileForPreview = async (file: FileVO) => {
  selectedFile.value = file
  await loadFilePreview(file)
}

// 处理文件选择（来自文件树组件的事件）
const handleFileSelected = async (fileInfo: { fileId: string, fileName: string, nodeName: string, nodeValue: any }) => {
  // 创建FileVO对象用于预览
  const fileVO: FileVO = {
    fileId: fileInfo.fileId,
    originalName: fileInfo.fileName,
    savedName: fileInfo.fileName,
    filePath: '', // 路径信息在nodeValue中
    fileSize: 0,
    fileType: '',
    uploadTime: '',
    downloadUrl: fileInfo.nodeValue?.url || '',
    userId: props.uid.toString(),
    sessionId: props.sid.toString()
  }
  
  selectedFileId.value = fileInfo.fileId
  await selectFileForPreview(fileVO)
}

// 加载文件预览
const loadFilePreview = async (file: FileVO) => {
  try {
    isLoading.value = true
    previewContent.value = ''
    previewUrl.value = ''
    previewType.value = 'unsupported'

    console.log('开始预览文件:', file.originalName, '文件类型:', file.fileType)

    // 首先检查文件是否在黑名单中
    if (!canPreviewFile(file.fileType || '', file.originalName)) {
      console.log('文件在预览黑名单中，不支持预览:', file.originalName)
      previewType.value = 'unsupported'
      return
    }

    // 判断文件类型并设置预览方式
    const fileType = file.fileType?.toLowerCase() || ''
    const fileName = file.originalName?.toLowerCase() || ''
    
    if (isImageFile(fileType, fileName)) {
      previewType.value = 'image'
      await loadImagePreview(file)
    } else if (isPdfFile(fileType, fileName)) {
      previewType.value = 'pdf'
      await loadPdfPreview(file)
    } else {
      // 默认按照文本文件处理，除非是黑名单中的文件
      previewType.value = 'text'
      await loadTextPreview(file)
      console.log('按文本格式预览文件:', fileName, '文件类型:', fileType)
    }

  } catch (error: any) {
    console.error('文件预览失败:', error)
    ElMessage.error('文件预览失败: ' + (error.message || '未知错误'))
    previewType.value = 'unsupported'
  } finally {
    isLoading.value = false
  }
}

// 判断是否为图片文件
const isImageFile = (fileType: string, fileName: string): boolean => {
  return fileType.startsWith('image/') || 
         ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg'].some(ext => fileName.endsWith(ext))
}

// 判断是否为PDF文件
const isPdfFile = (fileType: string, fileName: string): boolean => {
  return fileType.includes('pdf') || fileName.endsWith('.pdf')
}

// 加载图片预览
const loadImagePreview = async (file: FileVO) => {
  const response = await previewFile(file.fileId)
  const blob = new Blob([response.data], { type: response.headers['content-type'] || file.fileType || 'image/*' })
  previewUrl.value = URL.createObjectURL(blob)
  console.log('图片预览URL已生成')
}

// 加载PDF预览
const loadPdfPreview = async (file: FileVO) => {
  const response = await previewFile(file.fileId)
  const blob = new Blob([response.data], { type: 'application/pdf' })
  previewUrl.value = URL.createObjectURL(blob)
  console.log('PDF预览URL已生成')
}

// 加载文本预览
const loadTextPreview = async (file: FileVO) => {
  const response = await previewFile(file.fileId)
  const blob = new Blob([response.data], { type: 'text/plain' })
  const text = await blob.text()
  previewContent.value = text
  console.log('文本预览内容已加载，长度:', text.length)
}

// 下载文件
const downloadFile = async (file: FileVO) => {
  try {
    isDownloading.value = true
    console.log('开始下载文件:', file.originalName)
    
    // 使用与ChatInput一致的下载方式
    await apiDownloadFile(file.fileId, file.originalName)
    
    ElMessage.success('文件下载成功')
  } catch (error) {
    console.error('文件下载失败:', error)
    ElMessage.error('文件下载失败')
  } finally {
    isDownloading.value = false
  }
}

// 关闭预览
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

// 监听会话变化
watch(() => [props.uid, props.sid], async (newValues, oldValues) => {
  const [newUid, newSid] = newValues || [0, 0]
  const [oldUid, oldSid] = oldValues || [0, 0]
  
  if ((newUid !== oldUid || newSid !== oldSid) && newUid && newSid) {
    selectedFile.value = null
    previewContent.value = ''
    await loadFileList()
  }
}, { immediate: true })

// 监听可见性变化
watch(() => props.visible, async (visible) => {
  if (visible && props.uid && props.sid) {
    await loadFileList()
  }
})
</script>

<style scoped>
.file-preview-overlay {
  position: fixed;
  top: 0;
  left: 55%;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background-color: white;
  border-left: 1px solid #e4e7ed;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
}

.side-panel-container {
  width: 100%;
  height: 100%;
  display: flex;
  background-color: white;
}

.directory-panel {
  width: 250px;
  background-color: #fafafa;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.directory-panel.collapsed {
  width: 50px;
}

.directory-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #f5f7fa;
}

.directory-header.collapsed {
  flex-direction: column;
  justify-content: center;
  padding: 8px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  color: #409eff;
  font-size: 18px;
}

.title-text {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 4px;
}

.header-actions.vertical {
  flex-direction: column;
  gap: 8px;
}

.collapsed-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  align-items: center;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #606266;
  background-color: transparent;
  border: 1px solid transparent;
}

.menu-item:hover {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  color: #409eff;
}

.menu-item:active {
  background-color: #e4e7ed;
}

.menu-item.loading {
  pointer-events: none;
  opacity: 0.6;
}

.menu-item.loading .el-icon {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.directory-content {
  flex: 1;
  overflow: hidden;
}

.file-tree {
  height: 100%;
  overflow-y: auto;
  padding: 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #dcdfe6;
}

.empty-text {
  font-size: 14px;
  margin: 0 0 8px 0;
  color: #606266;
}

.empty-hint {
  font-size: 12px;
  margin: 0;
  color: #909399;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  margin-bottom: 4px;
}

.file-item:hover {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
}

.file-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.file-item:last-child {
  margin-bottom: 0;
}

.file-icon {
  margin-right: 8px;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 12px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: #909399;
}

.file-type {
  background-color: #f0f2f5;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 9px;
}

.file-size {
  font-size: 9px;
}

.separator {
  margin: 0 2px;
}

.file-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.file-item:hover .file-actions {
  opacity: 1;
}

.file-actions .el-button {
  width: 20px;
  height: 20px;
  min-width: 20px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.explorer-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.explorer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  background-color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.file-size {
  font-size: 12px;
}

.preview-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
  background-color: white;
}

/* 图片预览样式 */
.image-preview {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

/* PDF预览样式 */
.pdf-preview {
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f8f9fa;
}

.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 8px;
}

/* 文本预览样式 */
.text-preview {
  height: 100%;
}

.text-content {
  width: 100%;
  height: 100%;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fafafa;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}

.no-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.no-preview-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #dcdfe6;
}

.no-preview-text {
  font-size: 14px;
  margin: 0 0 16px 0;
  color: #606266;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #dcdfe6;
}

.placeholder-text {
  font-size: 14px;
  margin: 0;
  color: #606266;
}

/* 滚动条样式 */
.file-tree::-webkit-scrollbar,
.preview-content::-webkit-scrollbar,
.text-content::-webkit-scrollbar {
  width: 6px;
}

.file-tree::-webkit-scrollbar-track,
.preview-content::-webkit-scrollbar-track,
.text-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.file-tree::-webkit-scrollbar-thumb,
.preview-content::-webkit-scrollbar-thumb,
.text-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.file-tree::-webkit-scrollbar-thumb:hover,
.preview-content::-webkit-scrollbar-thumb:hover,
.text-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
