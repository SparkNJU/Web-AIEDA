import { axios } from '../utils/request'

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

// 下载文件 - 通过后端代理下载（推荐方式）
export const downloadFile = async (fid: string, fileName?: string) => {
  try {
    const response = await axios.get(`${FILE_MODULE}/download/${fid}`, {
      responseType: 'blob'
    })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    
    // 尝试从响应头获取文件名，否则使用传入的文件名或默认名称
    const contentDisposition = response.headers['content-disposition']
    let downloadFileName = fileName || 'download'
    
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="?([^"]+)"?/)
      if (match) {
        downloadFileName = match[1]
      }
    }
    
    link.download = downloadFileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
  } catch (error) {
    console.error('文件下载失败:', error)
    throw error
  }
}

// 下载文件 - 直接从大模型服务下载（已废弃，改为通过后端代理）
export const downloadFileFromLLM = (fid: string) => {
  console.warn('downloadFileFromLLM 已废弃，请使用 downloadFile 方法')
  // 为了向后兼容，调用新的下载方法
  return downloadFile(fid)
}

// 获取文件下载URL
export const getDownloadUrl = (fid: string) => {
  return axios.get(`${FILE_MODULE}/download-url/${fid}`)
}

// 预览文件 - 通过后端代理获取二进制流进行预览
export const previewFile = (fid: string) => {
  return axios.get(`${FILE_MODULE}/preview/${fid}`, {
    responseType: 'blob'
  })
}

// 预览文件（兼容接口）- 通过后端代理调用大模型服务
export const previewFileLegacy = (fid: string) => {
  return axios.get(`${FILE_MODULE}/preview-legacy/${fid}`)
}

// 获取文件预览URL
export const getPreviewUrl = (fid: string) => {
  return axios.get(`${FILE_MODULE}/preview-url/${fid}`)
}

// 获取文件列表
export const getFileList = (params: FileListRequestVO) => {
  return axios.get(`${FILE_MODULE}/list`, { params })
}

// 获取文件结构（从大模型服务）
export const getFileStructure = (params: FileListRequestVO) => {
  return axios.get(`${FILE_MODULE}/structure`, { params })
}

// 通过本地路径访问文件
export const getLocalFile = (uid: number, sid: number, filename: string) => {
  return axios.get(`${FILE_MODULE}/local/${uid}/${sid}/${filename}`, {
    responseType: 'blob'
  })
}

// 通过完整本地路径读取文件内容（文本类型）
export const getLocalFileByPath = (filePath: string) => {
  return axios.get(`${FILE_MODULE}/local-path`, {
    params: { path: filePath },
    responseType: 'text'
  })
}

// 通过完整本地路径下载文件
export const downloadLocalFileByPath = (filePath: string) => {
  return axios.get(`${FILE_MODULE}/download-local-path`, {
    params: { path: filePath },
    responseType: 'blob'
  })
}

// 获取本地文件预览内容（文本类型）
export const getLocalFilePreview = (uid: number, sid: number, filename: string) => {
  return axios.get(`${FILE_MODULE}/local/${uid}/${sid}/${filename}`, {
    responseType: 'text'
  })
}

// 删除文件
export const deleteFile = (fid: string) => {
  return axios.delete(`${FILE_MODULE}/${fid}`)
}

// 获取文件信息
export const getFileInfo = (fid: string) => {
  return axios.get(`${FILE_MODULE}/info/${fid}`)
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
  if (type.includes('word') || type.includes('doc') || type.includes('msword') || 
      type.includes('wordprocessingml')) return '📝'
  if (type.includes('excel') || type.includes('sheet') || type.includes('spreadsheet') || 
      type.includes('spreadsheetml')) return '📊'
  if (type.includes('powerpoint') || type.includes('presentation') || 
      type.includes('presentationml')) return '📺'
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
  
  // 可预览的文件类型（与上传支持的文件类型保持一致）
  const previewableTypes = [
    // 图片类型
    'image/',
    
    // 文本类型 - MIME 类型
    'text/', 'application/json', 'application/xml', 'text/csv',
    'text/plain', 'text/html', 'text/css', 'text/javascript',
    'application/javascript', 'text/markdown', 'text/x-markdown',
    
    // PDF文档
    'application/pdf',
    
    // Office文档（部分可预览）
    'application/msword', // .doc
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document', // .docx
    'application/vnd.ms-excel', // .xls
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
    'application/vnd.ms-powerpoint', // .ppt
    'application/vnd.openxmlformats-officedocument.presentationml.presentation', // .pptx
    
    // 其他格式
    'application/rtf'
  ]
  
  // 可预览的文件扩展名
  const previewableExtensions = [
    '.txt', '.md', '.json', '.xml', '.csv', '.html', '.css', '.js',
    '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.rtf'
  ]
  
  // 检查 MIME 类型或扩展名
  const isMimeTypeSupported = previewableTypes.some(previewType => 
    type.startsWith(previewType) || type.includes(previewType)
  )
  
  const isExtensionSupported = previewableExtensions.some(ext => 
    type === ext || type.endsWith(ext)
  )
  
  // 图片类型特殊处理：支持常见图片扩展名
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  const isImageExtension = imageExtensions.some(ext => type === ext || type.endsWith(ext))
  
  return isMimeTypeSupported || isExtensionSupported || isImageExtension
}
