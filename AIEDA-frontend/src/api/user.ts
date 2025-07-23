import { axios } from '../utils/request'
import { USER_MODULE } from './_prefix';
// 用户相关接口
type LoginInfo = {
    phone: string,
    password: string
}

type RegisterInfo = {
    role: number,
    username: string,
    phone: string,
    password: string,
}

type UpdateInfo = {
    username?: string,
    password?: string,
    description?: string,
}

// 如果有“Vue: This may be converted to an async function”警告，可以不管
// 用户登录
export const userLogin = (loginInfo: LoginInfo) => {
    return axios.post(`${USER_MODULE}/login`, loginInfo)
        .then(res => res)
}

// 用户注册
export const userRegister = (registerInfo: RegisterInfo) => {
    return axios.post(`${USER_MODULE}`, registerInfo, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res)
}

// 获取用户信息
export const userInfo = () => {
    const phone = sessionStorage.getItem('phone')
    return axios.get(`${USER_MODULE}/${phone}`)
        .then(res => res)
}

// 更新用户信息
export const userInfoUpdate = (updateInfo: UpdateInfo) => {
    return axios.put(`${USER_MODULE}`, updateInfo, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res)
}
