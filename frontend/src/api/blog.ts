import request from './request'
import type { ArticleItem, TagItem, ColumnItem, CarouselItem } from './blog.types'

export type { ArticleItem, TagItem, ColumnItem, CarouselItem }

const USE_MOCK = true // 无后端时改为 true；对接后端后改为 false

const mockTags: TagItem[] = [
  { id: '1', name: '技术' },
  { id: '2', name: '云原生' },
  { id: '3', name: 'Kubernetes' },
  { id: '4', name: '写作' },
  { id: '5', name: '产品' },
  { id: '6', name: 'AI' },
  { id: '7', name: '生活' },
]

const mockAuthor = { id: '1', nickname: '博主', avatar: '' }

const mockArticles: ArticleItem[] = [
  { id: '1', title: 'Purpose & Profit – 发现你一生的事业', summary: '全书免费阅读。', author: mockAuthor, createdAt: '2025-02-20', tags: ['写作'], cover: '' },
  { id: '2', title: '如何清晰而有深度地表达自己', summary: '沟通与逻辑的实践指南。', author: mockAuthor, createdAt: '2025-02-19', tags: ['写作'], cover: '' },
  { id: '3', title: '一天内理顺生活的办法', summary: '极简行动清单。', author: mockAuthor, createdAt: '2025-02-18', tags: ['生活'], cover: '' },
  { id: '4', title: '多兴趣者如何不浪费你的天赋', summary: '跨领域成长的地图。', author: mockAuthor, createdAt: '2025-02-17', tags: ['产品'], cover: '' },
  { id: '5', title: '像战略家一样思考（五维思考）', summary: '从单点到系统的思维升级。', author: mockAuthor, createdAt: '2025-02-16', tags: ['技术'], cover: '' },
  { id: '6', title: '云原生入门：从零到部署', summary: '容器、编排与可观测性。', author: mockAuthor, createdAt: '2025-02-15', tags: ['云原生', 'Kubernetes'], cover: '' },
]

const mockColumns: ColumnItem[] = [
  { id: '1', name: '云原生实战', description: 'K8s、Docker、服务网格', articleCount: 12 },
  { id: '2', name: '写作与表达', description: '结构化写作与演讲', articleCount: 8 },
  { id: '3', name: 'AI 与工具', description: 'Prompt 与自动化', articleCount: 5 },
]

/** 获取用户博客标签列表 */
export function getBlogTags(): Promise<TagItem[]> {
  if (USE_MOCK) return Promise.resolve(mockTags)
  return request.get('/blog/tags').then((data: unknown) => (data as TagItem[]) || [])
}

/** 获取轮播/精选文章（用于轮播图） */
export function getCarouselArticles(): Promise<CarouselItem[]> {
  if (USE_MOCK) return Promise.resolve(mockArticles.slice(0, 3))
  return request.get('/blog/carousel').then((data: unknown) => (data as CarouselItem[]) || [])
}

/** 获取热门文章（Most Popular） */
export function getPopularArticles(limit = 4): Promise<ArticleItem[]> {
  if (USE_MOCK) return Promise.resolve(mockArticles.slice(0, limit))
  return request.get('/blog/popular', { params: { limit } }).then((data: unknown) => (data as ArticleItem[]) || [])
}

/** 获取文章列表（Latest/Top 等） */
export function getArticleList(params?: { tagId?: string; tab?: string; page?: number; pageSize?: number }): Promise<{ list: ArticleItem[]; total: number }> {
  if (USE_MOCK) return Promise.resolve({ list: mockArticles, total: mockArticles.length })
  return request.get('/blog/articles', { params }).then((data: unknown) => {
    const d = data as { list?: ArticleItem[]; total?: number }
    return { list: d?.list || [], total: d?.total ?? 0 }
  })
}

/** 获取用户专栏列表 */
export function getColumns(limit = 6): Promise<ColumnItem[]> {
  if (USE_MOCK) return Promise.resolve(mockColumns)
  return request.get('/blog/columns', { params: { limit } }).then((data: unknown) => (data as ColumnItem[]) || [])
}
