import { axios, BASE_URL, getToken } from '../utils/request'

// 文件相关接口的前缀
const FILE_MODULE = '/api/files'

// 定义文件相关的数据类型
export interface FileVO {
  fileId: string
  originalName: string
  savedName: string
  filePath: string
  fileSize: number
  fileType: string
  uploadTime: string
  downloadUrl: string
  userId: string
  sessionId: string
}

export interface FileUploadRequestVO {
  uid: number
  sid: number
  file: File
  metadata?: string
}

export interface FileListRequestVO {
  uid: number
  sid: number
}

export interface FilePreviewVO {
  fileId: string
  originalName: string
  fileType: string
  fileSize: number
  previewUrl?: string
  previewContent?: string
  canPreview: boolean
}

export interface FileListResponseVO {
  files: FileVO[]
  totalCount: number
}

// 文件上传
export const uploadFile = (data: FileUploadRequestVO) => {
  const formData = new FormData()
  formData.append('file', data.file)
  formData.append('uid', data.uid.toString())
  formData.append('sid', data.sid.toString())
  if (data.metadata) {
    formData.append('metadata', data.metadata)
  }

  return axios.post(`${FILE_MODULE}/upload`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 下载文件
export const downloadFile = (fid: string): Promise<Blob> => {
  return axios.get(`${FILE_MODULE}/download/${fid}`, {
    responseType: 'blob'
  })
}

// 预览文件
export const previewFile = (fid: string) => {
  return axios.get(`${FILE_MODULE}/preview/${fid}`)
}

// 获取文件列表
export const getFileList = (params: FileListRequestVO) => {
  return axios.get(`${FILE_MODULE}/list`, { params })
}

// 删除文件
export const deleteFile = (fid: string) => {
  return axios.delete(`${FILE_MODULE}/${fid}`)
}

// 获取文件信息
export const getFileInfo = (fid: string) => {
  return axios.get(`${FILE_MODULE}/info/${fid}`)
}

// 带文件引用的流式聊天
export const sendMessageWithFilesStream = async (data: {
  uid: number
  sid: number
  content: string
  fileReferences?: string[]
}): Promise<Response> => {
  const token = getToken()
  const headers: HeadersInit = {
    'Content-Type': 'application/json'
  }
  
  if (token) {
    headers['token'] = token
  }
  
  const response = await fetch(`${BASE_URL}/api/chat/messages/stream-with-files`, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(data)
  })
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  
  return response
}

// 格式化文件大小
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 获取文件图标
export const getFileIcon = (fileType: string): string => {
  const type = fileType.toLowerCase()
  
  if (type.includes('image')) return '🖼️'
  if (type.includes('pdf')) return '📄'
  if (type.includes('word') || type.includes('doc') || type.includes('msword')) return '📝'
  if (type.includes('excel') || type.includes('sheet') || type.includes('spreadsheet')) return '📊'
  if (type.includes('powerpoint') || type.includes('presentation')) return '📺'
  if (type.includes('text') || type.includes('txt') || type.includes('plain')) return '📃'
  if (type.includes('markdown') || type.includes('md')) return '📋'
  if (type.includes('json')) return '🔧'
  if (type.includes('xml')) return '🏷️'
  if (type.includes('csv')) return '📈'
  if (type.includes('code') || type.includes('javascript') || type.includes('python') || type.includes('java')) return '💻'
  if (type.includes('zip') || type.includes('rar') || type.includes('7z')) return '🗜️'
  if (type.includes('video')) return '🎥'
  if (type.includes('audio')) return '🎵'
  
  return '📁'
}

// 判断文件是否可预览
export const canPreviewFile = (fileType: string): boolean => {
  const type = fileType.toLowerCase()
  
  // 可预览的文件类型
  const previewableTypes = [
    // 文本类型
    'text/', 'application/json', 'application/xml', 'text/csv',
    'text/plain', 'text/html', 'text/css', 'text/javascript',
    'application/javascript', 'text/markdown', 'text/x-markdown',
    
    // 图片类型
    'image/',
    
    // 文档类型
    'application/pdf',
    
    // 其他常见格式
    'application/rtf'
  ]
  
  return previewableTypes.some(previewType => type.startsWith(previewType) || type.includes(previewType))
}
