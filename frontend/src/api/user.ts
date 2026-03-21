import request from './request'

export interface UserMe {
  id: number
  username: string
  nickname?: string
  avatar?: string
  cover?: string
  gender?: string
  intro?: string
  residence?: string
  industry?: string
  bio?: string
  /** 手机号（后端可脱敏展示） */
  phone?: string
  role?: string
  createdAt?: string
}

export interface UpdateProfilePayload {
  nickname?: string
  avatar?: string
  cover?: string
  gender?: string
  intro?: string
  residence?: string
  industry?: string
  bio?: string
  phone?: string
}

export function getMe(): Promise<UserMe> {
  return request.get<any>('users/me').then((data) => data as UserMe)
}

/** 按昵称或用户名模糊搜索（公开），用于搜索页「用户」 */
export function searchUsers(q: string): Promise<UserMe[]> {
  if (!q?.trim()) return Promise.resolve([])
  return request
    .get<UserMe[]>('users/search', { params: { q: q.trim() } })
    .then((data) => (Array.isArray(data) ? data : []))
}

/** 按 ID 获取用户公开资料（昵称、头像等），用于文章页作者展示等；无需登录 */
export function getUserById(id: number): Promise<UserMe> {
  return request.get<any>(`users/${id}`).then((data) => data as UserMe)
}

/** 批量获取用户公开资料，用于关注页列表等；无需登录 */
export function getUsersBatch(ids: number[]): Promise<UserMe[]> {
  if (!ids?.length) return Promise.resolve([])
  return request
    .get<any>('users/batch', { params: { ids } })
    .then((data) => (Array.isArray(data) ? data : []))
}

export function updateMe(payload: UpdateProfilePayload): Promise<UserMe> {
  return request.patch<any>('users/me', payload).then((data) => data as UserMe)
}
