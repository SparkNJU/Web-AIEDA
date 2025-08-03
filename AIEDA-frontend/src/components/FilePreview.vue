<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElCard, ElButton, ElIcon, ElTag, ElMessage } from 'element-plus'
import { Download, Close, Document } from '@element-plus/icons-vue'
import { previewFile, downloadFile, getFileInfo, type FileVO, type FilePreviewVO, formatFileSize, getFileIcon, canPreviewFile } from '../api/file'

// 组件属性
const props = defineProps<{
  fileId?: string
  file?: FileVO
  visible: boolean
}>()

// 组件事件
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'close': []
}>()

// 响应式数据
const fileInfo = ref<FileVO | null>(props.file || null)
const previewData = ref<FilePreviewVO | null>(null)
const isLoading = ref(false)
const previewContent = ref('')
const isDownloading = ref(false)

// 计算属性
const currentFileId = computed(() => props.fileId || props.file?.fileId)

const isVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const fileIcon = computed(() => {
  return fileInfo.value ? getFileIcon(fileInfo.value.fileType) : '📁'
})

const canPreview = computed(() => {
  return fileInfo.value ? canPreviewFile(fileInfo.value.fileType) : false
})

const isImageFile = computed(() => {
  return fileInfo.value?.fileType.startsWith('image/') || false
})

const isTextFile = computed(() => {
  if (!fileInfo.value) return false
  const type = fileInfo.value.fileType.toLowerCase()
  return type.startsWith('text/') || 
         type.includes('json') || 
         type.includes('xml') ||
         type.includes('javascript') ||
         type.includes('markdown')
})

const isPdfFile = computed(() => {
  return fileInfo.value?.fileType === 'application/pdf' || false
})

// 监听文件ID变化
watch(() => currentFileId.value, async (newFileId) => {
  if (newFileId && props.visible) {
    await loadFileInfo()
    await loadPreview()
  }
}, { immediate: true })

// 监听可见性变化
watch(() => props.visible, async (visible) => {
  if (visible && currentFileId.value) {
    if (!fileInfo.value) {
      await loadFileInfo()
    }
    await loadPreview()
  }
})

// 加载文件信息
const loadFileInfo = async () => {
  if (!currentFileId.value || props.file) return

  try {
    isLoading.value = true
    const response = await getFileInfo(currentFileId.value)
    if (response.data && response.data.status === 'success') {
      fileInfo.value = response.data.data
    }
  } catch (error) {
    console.error('加载文件信息失败:', error)
    ElMessage.error('加载文件信息失败')
  } finally {
    isLoading.value = false
  }
}

// 加载预览内容
const loadPreview = async () => {
  if (!currentFileId.value || !canPreview.value) return

  try {
    isLoading.value = true
    const response = await previewFile(currentFileId.value)
    
    if (response.data && response.data.status === 'success') {
      previewData.value = response.data.data
      previewContent.value = response.data.data.previewContent || ''
    }
  } catch (error) {
    console.error('加载文件预览失败:', error)
    ElMessage.error('文件预览加载失败')
  } finally {
    isLoading.value = false
  }
}

// 下载文件
const handleDownload = async () => {
  if (!currentFileId.value || !fileInfo.value) return

  try {
    isDownloading.value = true
    const blob = await downloadFile(currentFileId.value)
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileInfo.value.originalName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
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
  isVisible.value = false
  emit('close')
}

// 获取图片预览URL
const getImagePreviewUrl = () => {
  if (!currentFileId.value) return ''
  return `/api/files/download/${currentFileId.value}`
}

// 获取PDF预览URL
const getPdfPreviewUrl = () => {
  if (!currentFileId.value) return ''
  return `/api/files/preview/${currentFileId.value}`
}
</script>

<template>
  <div v-if="isVisible" class="file-preview-panel">
    <el-card class="preview-card" shadow="never">
      <!-- 文件预览头部 -->
      <template #header>
        <div class="preview-header">
          <div class="file-info">
            <span class="file-icon-large">{{ fileIcon }}</span>
            <div class="file-details">
              <div class="file-name" :title="fileInfo?.originalName">
                {{ fileInfo?.originalName || '未知文件' }}
              </div>
              <div class="file-meta">
                <el-tag size="small" type="info">{{ fileInfo?.fileType || 'unknown' }}</el-tag>
                <span class="file-size">{{ fileInfo ? formatFileSize(fileInfo.fileSize) : '-' }}</span>
              </div>
            </div>
          </div>
          
          <div class="preview-actions">
            <el-button 
              type="primary" 
              :icon="Download" 
              @click="handleDownload"
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
            >
              关闭
            </el-button>
          </div>
        </div>
      </template>

      <!-- 文件预览内容 -->
      <div class="preview-content" v-loading="isLoading">
        <!-- 可预览的文件 -->
        <template v-if="canPreview && !isLoading">
          <!-- 图片预览 -->
          <div v-if="isImageFile" class="image-preview">
            <img 
              :src="getImagePreviewUrl()" 
              :alt="fileInfo?.originalName"
              class="preview-image"
              @error="() => ElMessage.error('图片加载失败')"
            />
          </div>

          <!-- 文本文件预览 -->
          <div v-else-if="isTextFile" class="text-preview">
            <pre class="text-content">{{ previewContent }}</pre>
          </div>

          <!-- PDF预览 -->
          <div v-else-if="isPdfFile" class="pdf-preview">
            <iframe 
              :src="getPdfPreviewUrl()"
              class="pdf-frame"
              frameborder="0"
            ></iframe>
          </div>

          <!-- 其他可预览文件 -->
          <div v-else class="general-preview">
            <div class="preview-placeholder">
              <el-icon class="preview-icon"><Document /></el-icon>
              <p>该文件类型支持预览，但暂未实现具体的预览功能</p>
              <p class="preview-hint">请点击下载按钮下载文件查看内容</p>
            </div>
          </div>
        </template>

        <!-- 不可预览的文件 -->
        <template v-else-if="!isLoading">
          <div class="no-preview">
            <el-icon class="no-preview-icon"><Document /></el-icon>
            <p class="no-preview-text">该文件类型不支持在线预览</p>
            <p class="no-preview-hint">请下载文件到本地查看</p>
            <el-button 
              type="primary" 
              :icon="Download" 
              @click="handleDownload"
              :loading="isDownloading"
            >
              立即下载
            </el-button>
          </div>
        </template>

        <!-- 加载状态 -->
        <template v-if="isLoading">
          <div class="loading-state">
            <p>正在加载文件预览...</p>
          </div>
        </template>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.file-preview-panel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.preview-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.preview-card :deep(.el-card__body) {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-icon-large {
  font-size: 32px;
  flex-shrink: 0;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.preview-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.preview-content {
  height: 100%;
  overflow: auto;
  position: relative;
}

/* 图片预览样式 */
.image-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 20px;
  background-color: #f8f9fa;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 文本预览样式 */
.text-preview {
  height: 100%;
  padding: 20px;
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

/* PDF预览样式 */
.pdf-preview {
  height: 100%;
  padding: 20px;
}

.pdf-frame {
  width: 100%;
  height: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

/* 通用预览样式 */
.general-preview,
.no-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;
  text-align: center;
  color: #909399;
}

.preview-icon,
.no-preview-icon {
  font-size: 64px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.preview-hint,
.no-preview-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
}

.no-preview-text {
  font-size: 16px;
  margin-bottom: 8px;
}

/* 加载状态样式 */
.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #909399;
}

/* 滚动条样式 */
.preview-content::-webkit-scrollbar,
.text-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.preview-content::-webkit-scrollbar-track,
.text-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.preview-content::-webkit-scrollbar-thumb,
.text-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.preview-content::-webkit-scrollbar-thumb:hover,
.text-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .preview-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .file-info {
    justify-content: center;
  }

  .preview-actions {
    justify-content: center;
  }

  .image-preview,
  .text-preview,
  .pdf-preview {
    padding: 12px;
  }

  .general-preview,
  .no-preview {
    padding: 20px;
  }

  .file-icon-large {
    font-size: 28px;
  }

  .preview-icon,
  .no-preview-icon {
    font-size: 48px;
  }
}
</style>
