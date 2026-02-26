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

/**
 * 获取当前用户的专栏列表（我的专栏）
 */
export function getColumnsMe(): Promise<ColumnItem[]> {
  return request.get<ColumnItem[]>('columns/me').then((data) => data as ColumnItem[])
}

/**
 * 创建专栏
 */
export function createColumn(body: CreateColumnBody): Promise<ColumnItem> {
  return request.post<ColumnItem>('columns', body).then((data) => data as ColumnItem)
}
