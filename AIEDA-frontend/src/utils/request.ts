import axios from 'axios'

const service = axios.create({
    baseURL: "http://localhost:8080",
    timeout: 30000
})

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

export {
    service as axios,
    getToken
}
