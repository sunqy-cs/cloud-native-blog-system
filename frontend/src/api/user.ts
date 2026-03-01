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
  wechatId?: string
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
  wechatId?: string
}

export function getMe(): Promise<UserMe> {
  return request.get<any>('users/me').then((data) => data as UserMe)
}

/** 按 ID 获取用户公开资料（昵称、头像等），用于文章页作者展示等；无需登录 */
export function getUserById(id: number): Promise<UserMe> {
  return request.get<any>(`users/${id}`).then((data) => data as UserMe)
}

export function updateMe(payload: UpdateProfilePayload): Promise<UserMe> {
  return request.patch<any>('users/me', payload).then((data) => data as UserMe)
}
