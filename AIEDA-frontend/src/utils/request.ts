import axios from 'axios'

// 导出基础URL配置，方便其他地方使用
export const BASE_URL = "http://localhost:8080"
// export const BASE_URL = ""

const service = axios.create({
    baseURL: BASE_URL,
    timeout: 30000 // 默认30秒超时
})

// 创建一个支持自定义超时的axios实例
const createServiceWithTimeout = (timeout: number) => {
    const customService = axios.create({
        baseURL: BASE_URL,
        timeout: timeout
    })

    // 为自定义实例添加请求拦截器
    customService.interceptors.request.use(
        config => {
            const token = getToken()
            if (token) {
                if (!config.headers) {
                    config.headers = {} as any
                }
                config.headers['token'] = token
            }
            return config
        },
        error => {
            console.error(error)
            return Promise.reject(error)
        }
    )

    // 为自定义实例添加响应拦截器
    customService.interceptors.response.use(
        response => {
            // 登录接口，自动存储token
            if (response.config.url && response.config.url.includes('/login') && response.data.code === '200') {
                sessionStorage.setItem('token', response.data.data)
            }
            // 统一判断后端code
            if (response.data && response.data.code === '200') {
                return response
            } else {
                return Promise.reject(response.data)
            }
        },
        error => {
            console.error(error)
            return Promise.reject(error)
        }
    )

    return customService
}

// 获取token
function getToken() {
    const token = sessionStorage.getItem('token')
    return token && token !== '' ? token : null
}

// 请求拦截器：自动携带token
service.interceptors.request.use(
    config => {
        const token = getToken()
        if (token) {
            if (!config.headers) {
                config.headers = {} as any
            }
            config.headers['token'] = token
        }
        return config
    },
    error => {
        console.error(error)
        return Promise.reject(error)
    }
)

// 响应拦截器：统一处理后端code为200才算成功
service.interceptors.response.use(
    response => {
        // 登录接口，自动存储token
        if (response.config.url && response.config.url.includes('/login') && response.data.code === '200') {
            // 登录成功，保存token
            sessionStorage.setItem('token', response.data.data)
        }
        // 统一判断后端code
        if (response.data && response.data.code === '200') {
            return response
        } else {
            // 失败情况
            return Promise.reject(response.data)
        }
    },
    error => {
        console.error(error)
        return Promise.reject(error)
    }
)

// 创建支持单次请求自定义超时的方法
const requestWithTimeout = (config: any, timeout: number) => {
    return service.request({
        ...config,
        timeout: timeout
    })
}

export {
    service as axios,
    createServiceWithTimeout,
    requestWithTimeout,
    getToken
}
