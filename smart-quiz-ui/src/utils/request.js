import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 统一 axios 实例：baseURL 走 Vite 代理 /api → 后端 8080
const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

const TOKEN_KEY = 'sq_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

// 请求拦截：注入 Bearer 令牌
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：拆包 Result{code,message,data}
request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端全局异常：HTTP 200 但 body.code != 200
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || 'Error'))
    }
    // 非标准返回体（如文件流），原样返回
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      setToken(null)
      ElMessage.error('登录已过期，请重新登录')
      const redirect = router.currentRoute.value.fullPath
      if (router.currentRoute.value.path !== '/login') {
        router.replace({ path: '/login', query: { redirect } })
      }
    } else {
      const msg = error.response?.data?.message || error.message || '网络错误'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request
