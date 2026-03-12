import axios from 'axios'
import { ElMessage } from 'element-plus'

const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8999/api'

const request = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (res) => {
    // 如果是 Blob 类型（通常是文件下载），直接返回响应对象，不进行业务 Code 校验
    if (res.request.responseType === 'blob' || res.data instanceof Blob) {
      return res
    }

    const payload = res?.data
    const code = payload?.code
    const msg = payload?.msg ?? '请求失败'

    if (code !== 200) {
      ElMessage.error(msg)
      return Promise.reject(payload)
    }

    return {
      code: payload.code,
      msg: payload.msg,
      data: payload.data,
    }
  },
  (error) => {
    const msg =
      error?.response?.data?.msg ||
      error?.message ||
      '网络异常，请稍后重试'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default request
