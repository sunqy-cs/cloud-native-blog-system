import request from './request'

export interface ContentListItem {
  id: number
  title: string
  summary?: string
  cover?: string | null
  status?: string   // DRAFT | PUBLISHED
  articleType?: string // ORIGINAL | REPRINT | TRANSLATED
  viewCount: number
  likeCount: number
  collectionCount: number
  commentCount?: number
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
  status?: 'ALL' | 'PUBLISHED' | 'REJECTED' | 'DRAFT'
  sortBy?: 'time' | 'likes' | 'views'
  order?: 'asc' | 'desc'
}

/** 创作者中心：当前用户内容汇总统计及昨日增长 */
export interface ContentMeStats {
  totalViewCount: number
  totalLikeCount: number
  totalCollectionCount: number
  yesterdayViewDelta: number
  yesterdayLikeDelta: number
  yesterdayCollectionDelta: number
}

/**
 * 获取当前用户内容统计（总阅读/总点赞/收藏及昨日增长），用于创作者中心数据卡片
 */
export function getContentMeStats(): Promise<ContentMeStats> {
  return request.get<ContentMeStats>('contents/me/stats').then((data) => data as ContentMeStats)
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
