import request from './request'

/** 有评论的文章（评论管理左侧） */
export interface CommentedArticle {
  contentId: number
  title: string
  commentCount: number
  lastCommentAt: string
}

/** 单条评论（评论管理右侧） */
export interface CommentItem {
  id: number
  userId: number
  userNickname: string
  contentId: number
  body: string
  parentId: number | null
  isHot: boolean
  createdAt: string
  isAuthor: boolean
}

/**
 * 获取有评论的文章列表（评论管理左侧）
 */
export function getCommentedArticles(): Promise<CommentedArticle[]> {
  return request
    .get<CommentedArticle[]>('comments/commented-articles')
    .then((data) => (Array.isArray(data) ? data : []))
}

/**
 * 获取某篇文章的评论列表（评论管理右侧）
 */
export function getContentComments(contentId: number): Promise<CommentItem[]> {
  return request
    .get<CommentItem[]>('comments/list', { params: { contentId } })
    .then((data) => (Array.isArray(data) ? data : []))
}

/**
 * 设置/取消热评（仅文章作者）
 */
export function setCommentHot(commentId: number, hot: boolean): Promise<void> {
  return request.patch(`comments/${commentId}/hot`, { hot })
}
