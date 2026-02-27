import request from './request'

export interface BlogBotItem {
  id: number
  name: string
  avatar?: string | null
  style: string
  mainTagId?: number | null
  mainTagName?: string | null
  summaryStyle: string
  wordCountPreference: string
  createdAt: string
  updatedAt: string
}

export interface CreateBlogBotBody {
  name: string
  avatar?: string
  style: string
  mainTagId?: number
  summaryStyle: string
  wordCountPreference: string
}

/**
 * 获取当前用户的博客机器人列表
 */
export function getBlogBotsMe(): Promise<BlogBotItem[]> {
  return request.get('blog-bots/me').then((data) => (data as unknown) as BlogBotItem[])
}

/**
 * 创建博客机器人
 */
export function createBlogBot(body: CreateBlogBotBody): Promise<BlogBotItem> {
  return request.post('blog-bots', body).then((data) => (data as unknown) as BlogBotItem)
}
