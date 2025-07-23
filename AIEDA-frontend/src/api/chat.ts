import { axios } from '../utils/request'
import { CHAT_MODULE } from './_prefix';

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
}

export interface CreateSessionRequestVO {
  uid: number
  title: string
}

// 聊天相关 API
export const sendMessage = (data: ChatRequestVO) => {
  return axios.post(`${CHAT_MODULE}/messages`, data)
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