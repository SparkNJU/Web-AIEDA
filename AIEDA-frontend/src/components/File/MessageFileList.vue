<!-- MessageFileList.vue - 消息中的文件列表组件 -->
<template>
  <div v-if="files.length > 0" class="message-files-area">
    <div class="message-files-carousel">
      <div 
        v-for="file in files" 
        :key="file.fileId"
        class="message-file-item"
        :title="file.originalName"
        @click="$emit('preview-file', file)"
      >
        <div class="file-icon-wrapper">
          <span class="file-icon">{{ getFileIcon(file.fileType) }}</span>
        </div>
        <div class="file-content">
          <div class="file-name">{{ file.originalName }}</div>
          <div class="file-meta">
            <span class="file-type">{{ getFileExtension(file.originalName) }}</span>
            <span class="separator">·</span>
            <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
          </div>
        </div>
        
        <!-- 文件操作按钮 -->
        <div class="file-actions">
          <button
            class="action-btn preview-btn"
            @click.stop="$emit('preview-file', file)"
            title="预览"
          >
            <el-icon><View /></el-icon>
          </button>
          <button
            class="action-btn download-btn"
            @click.stop="$emit('download-file', file)"
            title="下载"
          >
            <el-icon><Download /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FileVO } from '../../api/file'
import { getFileIcon, formatFileSize } from '../../api/file'
import { View, Download } from '@element-plus/icons-vue'
// 定义props
defineProps<{
  files: FileVO[]
}>()

// 定义事件
defineEmits<{
  'preview-file': [file: FileVO]
  'download-file': [file: FileVO]
}>()

// 获取文件扩展名
const getFileExtension = (filename: string) => {
  const lastDot = filename.lastIndexOf('.')
  return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : 'file'
}
</script>

<style scoped>
.message-files-area {
  margin-bottom: 12px;
  padding: 8px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  border: 1px dashed rgba(102, 8, 116, 0.2);
}

.message-files-carousel {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.message-file-item {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 6px;
  padding: 6px 10px;
  min-width: 140px;
  max-width: 220px;
  border: 1px solid rgba(102, 8, 116, 0.15);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.message-file-item:hover {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(102, 8, 116, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 8, 116, 0.1);
}

.file-icon-wrapper {
  margin-right: 8px;
  flex-shrink: 0;
}

.file-icon {
  font-size: 18px;
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
  font-weight: 500;
}

.separator {
  margin: 0 2px;
}

.file-actions {
  display: none;
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 4px;
  padding: 2px;
  gap: 2px;
  backdrop-filter: blur(8px);
}

.message-file-item:hover .file-actions {
  display: flex;
}

.action-btn {
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 3px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  transition: all 0.2s ease;
  color: #666;
}

.action-btn:hover {
  background: rgba(102, 8, 116, 0.1);
  color: rgb(102, 8, 116);
}

.preview-btn:hover {
  background: rgba(0, 123, 255, 0.1);
  color: #007bff;
}

.download-btn:hover {
  background: rgba(40, 167, 69, 0.1);
  color: #28a745;
}
</style>
