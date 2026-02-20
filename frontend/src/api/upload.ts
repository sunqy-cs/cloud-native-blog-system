import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const baseURL = import.meta.env.VITE_API_BASE ?? '/api'

export interface ObjectMeta {
  key: string
  url: string
  size: number
  contentType: string
}

/**
 * 上传图片到对象存储（走网关 -> file-service）
 * 需要已登录，请求头会带 Token
 */
export function uploadImage(file: File, prefix?: string): Promise<ObjectMeta> {
  const form = new FormData()
  form.append('file', file)
  if (prefix) form.append('prefix', prefix)
  const userStore = useUserStore()
  const headers: Record<string, string> = {}
  if (userStore.token) headers.Authorization = `Bearer ${userStore.token}`
  return axios
    .post<ObjectMeta>(`${baseURL}/objects`, form, { headers, timeout: 30000 })
    .then((res) => res.data)
    .catch((err) => {
      const msg = err.response?.data?.message ?? err.message ?? '上传失败'
      ElMessage.error(msg)
      throw err
    })
}
