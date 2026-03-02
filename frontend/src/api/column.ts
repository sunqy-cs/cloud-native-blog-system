import request from './request'

export interface ColumnItem {
  id: number
  /** 专栏所属用户 ID，仅搜索接口返回 */
  userId?: number
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
 * 按专栏名称或描述模糊搜索（公开），用于搜索页「专栏」
 */
export function searchColumns(q: string): Promise<ColumnItem[]> {
  if (!q?.trim()) return Promise.resolve([])
  return request
    .get<ColumnItem[]>('columns/search', { params: { q: q.trim() } })
    .then((data) => (Array.isArray(data) ? data : []))
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
 * 按 ID 获取专栏详情（公开），用于专栏详情页
 */
export function getColumnById(id: number): Promise<ColumnItem> {
  return request.get<ColumnItem>(`columns/${id}`).then((data) => data as ColumnItem)
}

/**
 * 将已发布文章加入专栏（仅本人的文章）
 */
export function addContentToColumn(columnId: number, contentId: number): Promise<void> {
  return request.post(`columns/${columnId}/contents`, { contentId })
}

/**
 * 从专栏移除文章（仅本人的专栏与文章）
 */
export function removeContentFromColumn(columnId: number, contentId: number): Promise<void> {
  return request.delete(`columns/${columnId}/contents`, { params: { contentId } })
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
