import request from './request'

export interface ContentLikeItem {
  contentId: number
  likedAt: string
}

export interface ContentLikesMeResponse {
  list: ContentLikeItem[]
  total: number
}

/**
 * 分页获取当前用户赞同的内容列表（我赞同的文章，用于动态）
 */
export function getContentLikesMe(params?: { page?: number; pageSize?: number }): Promise<ContentLikesMeResponse> {
  return request
    .get<ContentLikesMeResponse>('content-likes/me', { params })
    .then((data) => data as ContentLikesMeResponse)
}
