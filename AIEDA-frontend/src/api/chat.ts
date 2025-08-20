import { axios, createServiceWithTimeout, getToken, BASE_URL } from '../utils/request'
import { CHAT_MODULE } from './_prefix';

// 创建长超时的axios实例（用于sendMessage）
const longTimeoutAxios = createServiceWithTimeout(300000) // 300秒

// 定义接口数据类型
export interface SessionVO {
  sid: number
  title: string
  createTime: string
  updateTime: string
}

export interface RecordVO {
  rid: number
  sid: number
  direction: boolean // true=用户，false=AI
  content: string
  sequence: number
  type: number
  createTime: string
}

export interface ChatRequestVO {
  uid: number
  content: string
  sid: number
  fileReferences?: string[] // 添加文件引用
  agentType?: 'orchestrator' | 'dynamic' // 添加Agent类型参数
  inputType?: 'question' | 'config' | 'intervention' | 'delete' // 添加输入类型参数
  // LLM自定义配置字段（仅在inputType为config时使用）
  apiKey?: string // 自定义API Key
  baseUrl?: string // 自定义Base URL
  model?: string // 自定义模型
}

export interface CreateSessionRequestVO {
  uid: number
  title: string
}

// 聊天相关 API

// 流式发送消息 - 返回Response对象用于流式读取
export const sendMessageStream = async (data: ChatRequestVO): Promise<Response> => {
  const token = getToken()
  const headers: HeadersInit = {
    'Content-Type': 'application/json'
  }
  
  // 如果有token，添加到headers中
  if (token) {
    headers['token'] = token
  }
  
  // 使用带会话ID的URL路径
  const response = await fetch(`${BASE_URL}${CHAT_MODULE}/messages/${data.sid}/stream`, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(data)
  })
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  
  return response
}


export const createSession = (uid: number, title: string = '新会话') => {
  return axios.post(`${CHAT_MODULE}/sessions`, { uid, title })
}

export const getSessionRecords = (sid: number) => {
  return axios.get(`${CHAT_MODULE}/sessions/${sid}/records`)
}

export const getUserSessions = (uid: number) => {
  return axios.get(`${CHAT_MODULE}/sessions/${uid}`)
}

export const updateSessionTitle = (uid: number, sid: number, title: string) => {
  return axios.put(`${CHAT_MODULE}/sessions/${sid}`, { uid, title })
}

export const deleteSession = (sid: number, uid: number) => {
  return axios.delete(`${CHAT_MODULE}/sessions/${sid}`, { data: { uid } })
}