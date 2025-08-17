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

// 获取层次化文件结构（直接返回LLM的原始结构）
export const getHierarchicalFileStructure = (params: FileListRequestVO) => {
  return axios.get(`${FILE_MODULE}/structure-tree`, { params })
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

// 判断文件是否可预览（黑名单模式）
export const canPreviewFile = (fileType: string, fileName?: string): boolean => {
  const type = fileType.toLowerCase()
  const name = (fileName || '').toLowerCase()
  
  // 不可预览的文件类型黑名单（目前只屏蔽 zip 文件）
  const blacklistedTypes = [
    // 压缩文件
    'application/zip',
    'application/x-zip-compressed',
    'application/x-zip',
    'multipart/x-zip'
  ]
  
  // 不可预览的文件扩展名黑名单
  const blacklistedExtensions = [
    '.zip'
  ]
  
  // 检查 MIME 类型黑名单
  const isMimeTypeBlacklisted = blacklistedTypes.some(blacklistedType => 
    type === blacklistedType || type.includes(blacklistedType)
  )
  
  // 检查文件扩展名黑名单
  const isExtensionBlacklisted = blacklistedExtensions.some(ext => 
    name.endsWith(ext) || type === ext || type.endsWith(ext)
  )
  
  // 如果在黑名单中，则不可预览
  if (isMimeTypeBlacklisted || isExtensionBlacklisted) {
    return false
  }
  
  // 不在黑名单中的文件都允许预览
  return true
}
