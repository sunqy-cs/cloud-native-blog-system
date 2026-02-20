import axios from 'axios'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { ElMessage } from 'element-plus'

// 开发时走 Vite 代理到网关 8080；生产可设 VITE_API_BASE 指向网关（如 http://host:8080/api）
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    const msg = err.response?.data?.message ?? err.message ?? '请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
