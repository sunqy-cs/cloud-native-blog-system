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
  /** 标签名称列表；搜索（带 q）时可能返回，用于高亮 */
  tagNames?: string[]
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
  /** 搜索关键词：模糊匹配标题、摘要、标签 */
  q?: string
  /** 按专栏 ID 筛选（博客页 HOME=不传，点专栏=传该专栏 id，便于扩展按用户后也可用） */
  columnId?: number
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
  return request.get('contents/me/stats').then((data) => data as unknown as ContentMeStats)
}

/**
 * 分页获取当前用户的博客列表（我的博客）
 */
export function getContentsMe(params?: ContentsMeParams): Promise<ContentsMeResponse> {
  return request.get('contents/me', { params }).then((data) => data as unknown as ContentsMeResponse)
}

/** 公开推荐列表参数（推荐页顶区与各主标签栏） */
export interface ContentsListParams {
  mainTagId?: number
  page?: number
  pageSize?: number
  sortBy?: 'time' | 'likes' | 'views'
  order?: 'asc' | 'desc'
}

/**
 * 公开推荐列表：已发布博客，可选按主标签筛选
 */
export function getContentsList(params?: ContentsListParams): Promise<ContentsMeResponse> {
  return request.get('contents/list', { params }).then((data) => data as unknown as ContentsMeResponse)
}

/**
 * 按 ID 批量获取内容摘要（用于动态里展示赞同内容的标题等）
 */
export function getContentsByIds(ids: number[]): Promise<ContentListItem[]> {
  if (!ids.length) return Promise.resolve([])
  const params = { ids: ids.join(',') }
  return request.get<ContentListItem[]>('contents/by-ids', { params }).then((data) => (Array.isArray(data) ? data : []))
}

/** 编辑用内容详情（含正文、标签等） */
export interface ContentDetail {
  id: number
  title: string
  body: string
  summary?: string
  cover?: string
  columnId?: number
  articleType?: string
  creationStatement?: string
  visibility?: string
  tagNames?: string[]
}

/** 公开阅读用文章详情（已发布），含阅读数、点赞数、作者 id */
export interface ContentView {
  id: number
  title: string
  body: string
  summary?: string
  cover?: string
  columnId?: number
  articleType?: string
  creationStatement?: string
  visibility?: string
  tagNames?: string[]
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: string
  userId: number
}

/**
 * 获取文章公开阅读详情（已发布博客，无需登录；会增加阅读数）
 */
export function getContentView(id: number): Promise<ContentView> {
  return request.get(`contents/view/${id}`).then((data) => data as unknown as ContentView)
}

/**
 * 获取单篇内容详情（仅当前用户可编辑时使用）
 */
export function getContentForEdit(id: number): Promise<ContentDetail> {
  return request.get(`contents/${id}`).then((data) => data as unknown as ContentDetail)
}

export interface SaveDraftBody {
  /** 传则更新该草稿，不传则新建 */
  id?: number
  title?: string
  body: string
  summary?: string
  cover?: string
  columnId?: number
  articleType?: string
  creationStatement?: string
  visibility?: string
  /** 标签名称列表，最多 5 个；后端按名称查询或创建后关联 */
  tagNames?: string[]
}

export interface SaveDraftResponse {
  id: number
  title: string
  status: string
  createdAt: string
}

/**
 * 保存草稿：正文必填；标题为空时后端存为 [无标题]
 */
export function saveDraft(body: SaveDraftBody): Promise<SaveDraftResponse> {
  return request.post('contents/draft', body).then((data) => data as unknown as SaveDraftResponse)
}

export interface PublishResponse {
  id: number
  title: string
  status: string
  publishedAt: string
}

/**
 * 发布博客：将指定草稿设为已发布
 */
export function publishContent(id: number): Promise<PublishResponse> {
  return request.post(`contents/${id}/publish`).then((data) => data as unknown as PublishResponse)
}
