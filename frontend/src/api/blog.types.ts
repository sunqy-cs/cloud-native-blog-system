/** 文章（列表项） */
export interface ArticleItem {
  id: string
  title: string
  summary?: string
  cover?: string
  author: { id: string; nickname: string; avatar?: string }
  createdAt: string
  views?: number
  likes?: number
  comments?: number
  tags?: string[]
}

/** 标签 */
export interface TagItem {
  id: string
  name: string
}

/** 专栏 */
export interface ColumnItem {
  id: string
  name: string
  description?: string
  articleCount?: number
  cover?: string
}

/** 轮播项（精选文章） */
export type CarouselItem = ArticleItem
