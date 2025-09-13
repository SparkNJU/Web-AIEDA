<template>
  <div class="tree-node">
    <!-- 文件夹节点 -->
    <div 
      v-if="isFolder" 
      class="folder-node"
      :class="{ 'expanded': isExpanded }"
    >
      <div class="folder-header" @click="toggleExpanded">
        <el-icon class="folder-icon">
          <FolderOpened v-if="isExpanded" />
          <Folder v-else />
        </el-icon>
        <span class="folder-name">{{ nodeName }}</span>
        <span class="item-count">({{ childrenCount }})</span>
      </div>
      
      <!-- 子节点 -->
      <div v-if="isExpanded" class="folder-children">
        <FileTreeNodeRecursive 
          v-for="(childValue, childKey) in nodeValue" 
          :key="`${nodeName}-${childKey}`"
          :node-name="String(childKey)"
          :node-value="childValue"
          :selected-file-id="selectedFileId"
          @file-selected="$emit('file-selected', $event)"
        />
      </div>
    </div>
    
    <!-- 文件节点 -->
    <div 
      v-else 
      class="file-node"
      :class="{ 'active': isSelected }"
      @click.stop="selectFile"
    >
      <el-icon class="file-icon">
        <Document />
      </el-icon>
      <span class="file-name">{{ nodeName }}</span>
      <el-button 
        :icon="Download" 
        @click.stop="downloadFile"
        size="small"
        circle
        class="download-btn"
        title="下载文件"
      />
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'FileTreeNodeRecursive'
}
</script>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElIcon, ElButton } from 'element-plus'
import { Folder, FolderOpened, Document, Download } from '@element-plus/icons-vue'
import { downloadFile as apiDownloadFile } from '../../api/file'

// 组件属性
const props = defineProps<{
  nodeName: string
  nodeValue: any
  selectedFileId?: string
}>()

// 组件事件
const emit = defineEmits<{
  'file-selected': [fileInfo: { fileId: string, fileName: string, nodeName: string, nodeValue: any }]
}>()

// 响应式数据
const isExpanded = ref(false)

// 组件挂载时的调试信息
onMounted(() => {
  console.log(`FileTreeNodeRecursive 组件挂载: "${props.nodeName}", 是文件夹: ${isFolder.value}`)
})

// 监听展开状态变化
watch(isExpanded, (newValue, oldValue) => {
  console.log(`文件夹 "${props.nodeName}" 展开状态变化: ${oldValue} -> ${newValue}`)
})

// 监听选中的文件ID变化
watch(() => props.selectedFileId, (newFileId, oldFileId) => {
  if (newFileId !== oldFileId && !isFolder.value) {
    console.log(`选中文件ID变化，当前节点 "${props.nodeName}": ${oldFileId} -> ${newFileId}, 是否选中: ${isSelected.value}`)
  }
})

// 计算属性
const isFolder = computed(() => {
  if (!props.nodeValue || typeof props.nodeValue !== 'object') {
    return false
  }
  
  // 如果直接包含 file_id 和 url，则是文件
  if ('file_id' in props.nodeValue && 'url' in props.nodeValue) {
    return false
  }
  
  // 否则是文件夹（包含其他对象或为空对象）
  return true
})

const isSelected = computed(() => {
  if (isFolder.value) return false
  return props.nodeValue?.file_id === props.selectedFileId
})

const childrenCount = computed(() => {
  if (!isFolder.value || !props.nodeValue) return 0
  return Object.keys(props.nodeValue).length
})

// 方法
const toggleExpanded = () => {
  if (isFolder.value) {
    console.log(`文件夹 "${props.nodeName}" 展开状态切换：${isExpanded.value} -> ${!isExpanded.value}`)
    isExpanded.value = !isExpanded.value
    console.log(`文件夹 "${props.nodeName}" 当前展开状态：${isExpanded.value}`)
  }
}

const selectFile = () => {
  if (!isFolder.value && props.nodeValue?.file_id) {
    console.log(`选择文件: "${props.nodeName}" (ID: ${props.nodeValue.file_id})`)
    emit('file-selected', {
      fileId: props.nodeValue.file_id,
      fileName: props.nodeName,
      nodeName: props.nodeName,
      nodeValue: props.nodeValue
    })
    console.log(`文件选择事件已发出，文件: ${props.nodeName}`)
  }
}

const downloadFile = async () => {
  if (!isFolder.value && props.nodeValue?.file_id) {
    try {
      await apiDownloadFile(props.nodeValue.file_id, props.nodeName)
    } catch (error) {
      console.error('下载文件失败:', error)
    }
  }
}
</script>

<style scoped>
.tree-node {
  width: 100%;
}

.folder-node {
  margin-bottom: 2px;
}

.folder-header {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.folder-header:hover {
  background-color: #f5f7fa;
}

.folder-icon {
  margin-right: 6px;
  color: #409eff;
  font-size: 16px;
}

.folder-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-count {
  font-size: 11px;
  color: #909399;
  margin-left: 4px;
}

.folder-children {
  margin-left: 16px;
  border-left: 1px dashed #e4e7ed;
  padding-left: 8px;
}

.file-node {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  margin-bottom: 2px;
}

.file-node:hover {
  background-color: #f5f7fa;
}

.file-node:hover .download-btn {
  opacity: 1;
}

.file-node.active {
  background-color: #ecf5ff;
  border: 1px solid #409eff;
}

.file-icon {
  margin-right: 6px;
  color: #606266;
  font-size: 16px;
}

.file-name {
  flex: 1;
  font-size: 12px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.download-btn {
  opacity: 0;
  transition: opacity 0.2s;
  width: 20px;
  height: 20px;
  margin-left: 4px;
}

.download-btn .el-icon {
  font-size: 12px;
}

/* 夜间模式样式 */
[data-theme="dark"] .folder-header {
  color: #ffffff;
}

[data-theme="dark"] .folder-icon {
  color: #cbd5e1;
}

[data-theme="dark"] .item-count {
  color: #94a3b8;
}

[data-theme="dark"] .folder-children {
  border-left-color: #4a5568;
}

[data-theme="dark"] .file-node {
  background-color: transparent;
}

[data-theme="dark"] .file-node:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

[data-theme="dark"] .file-node.active {
  background-color: rgba(102, 8, 163, 0.2);
  border-color: rgba(102, 8, 163, 0.5);
}

[data-theme="dark"] .file-icon {
  color: #cbd5e1;
}

[data-theme="dark"] .file-name {
  color: #ffffff;
}
</style>
