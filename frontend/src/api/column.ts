import request from './request'

export interface ColumnItem {
  id: number
  name: string
  description?: string | null
  cover?: string | null
  articleCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateColumnBody {
  name: string
  description?: string
  cover?: string
}

export interface UpdateColumnBody {
  name?: string
  description?: string
  cover?: string
}

/**
 * 获取当前用户的专栏列表（我的专栏）
 */
export function getColumnsMe(): Promise<ColumnItem[]> {
  return request.get<ColumnItem[]>('columns/me').then((data) => data as ColumnItem[])
}

/**
 * 按用户 ID 获取专栏列表（公开），用于他人博客页「全部 / 专栏」导航
 */
export function getColumnsByUserId(userId: number): Promise<ColumnItem[]> {
  return request
    .get<ColumnItem[]>('columns/list', { params: { userId } })
    .then((data) => (Array.isArray(data) ? data : []))
}

/**
 * 创建专栏
 */
export function createColumn(body: CreateColumnBody): Promise<ColumnItem> {
  return request.post<ColumnItem>('columns', body).then((data) => data as ColumnItem)
}

/**
 * 更新专栏（名称、描述、封面）
 */
export function updateColumn(id: number, body: UpdateColumnBody): Promise<ColumnItem> {
  return request.patch<ColumnItem>(`columns/${id}`, body).then((data) => data as ColumnItem)
}

/**
 * 删除专栏
 */
export function deleteColumn(id: number): Promise<void> {
  return request.delete(`columns/${id}`)
}
