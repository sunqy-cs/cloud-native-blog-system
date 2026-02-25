import request from './request'

export interface ContentListItem {
  id: number
  title: string
  summary?: string
  cover?: string | null
  viewCount: number
  likeCount: number
  collectionCount: number
  createdAt: string
}

export interface ContentsMeResponse {
  list: ContentListItem[]
  total: number
}

export interface ContentsMeParams {
  page?: number
  pageSize?: number
  visibility?: 'ALL' | 'SELF' | 'FANS'
  sortBy?: 'time' | 'likes' | 'views'
  order?: 'asc' | 'desc'
}

/**
 * 分页获取当前用户的博客列表（我的博客）
 */
export function getContentsMe(params?: ContentsMeParams): Promise<ContentsMeResponse> {
  return request.get<ContentsMeResponse>('contents/me', { params }).then((data) => data as ContentsMeResponse)
}

/**
 * 按 ID 批量获取内容摘要（用于动态里展示赞同内容的标题等）
 */
export function getContentsByIds(ids: number[]): Promise<ContentListItem[]> {
  if (!ids.length) return Promise.resolve([])
  const params = { ids: ids.join(',') }
  return request.get<ContentListItem[]>('contents/by-ids', { params }).then((data) => (Array.isArray(data) ? data : []))
}
