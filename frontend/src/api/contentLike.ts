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

/** 查询当前用户是否已点赞指定内容（未登录返回 false） */
export function checkContentLiked(contentId: number): Promise<{ liked: boolean }> {
  return request
    .get<{ liked: boolean }>('content-likes/check', { params: { contentId } })
    .then((data) => data as { liked: boolean })
}

/** 点赞文章（需登录） */
export function likeContent(contentId: number): Promise<void> {
  return request.post('content-likes', { contentId })
}

/** 取消点赞文章（需登录） */
export function unlikeContent(contentId: number): Promise<void> {
  return request.delete(`content-likes/${contentId}`)
}
