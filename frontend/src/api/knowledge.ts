import request from './request'

export interface KnowledgeBaseItem {
  id: number
  name: string
  cover?: string | null
  description?: string | null
  visibility: 'PRIVATE' | 'PUBLIC'
  ownerId: number
  ownerName?: string | null
  ownerAvatar?: string | null
  subCount: number
  contentCount: number
  createdAt?: string
  updatedAt?: string
  subscribed?: boolean
}

export interface KnowledgeBaseContentItem {
  id: number
  title: string
  summary?: string | null
  cover?: string | null
}

export interface KnowledgeBaseContentsResponse {
  list: KnowledgeBaseContentItem[]
  total: number
}

export interface KnowledgeBaseListResponse {
  list: KnowledgeBaseItem[]
  total: number
}

export interface CreateKnowledgeBaseBody {
  name: string
  description?: string
  cover?: string
  visibility?: 'PRIVATE' | 'PUBLIC'
}

export interface UpdateKnowledgeBaseBody {
  name?: string
  description?: string
  cover?: string
  visibility?: 'PRIVATE' | 'PUBLIC'
}

/** 获取当前用户的知识库列表（我的知识库） */
export function getKnowledgeBasesMe(): Promise<KnowledgeBaseItem[]> {
  return request
    .get<KnowledgeBaseItem[]>('knowledge-bases/me')
    .then((data) => (Array.isArray(data) ? data : []))
}

/** 热门知识库列表（公开，按订阅数排序，可传 q 搜索） */
export function getKnowledgeBasesPopular(params?: {
  page?: number
  pageSize?: number
  q?: string
}): Promise<KnowledgeBaseListResponse> {
  return request
    .get<KnowledgeBaseListResponse>('knowledge-bases/popular', { params })
    .then((data) => (data as unknown) as KnowledgeBaseListResponse)
}

/** 获取当前用户订阅的知识库列表（我的订阅） */
export function getKnowledgeBasesSubscribed(): Promise<KnowledgeBaseItem[]> {
  return request
    .get<KnowledgeBaseItem[]>('knowledge-bases/subscribed')
    .then((data) => (Array.isArray(data) ? data : []))
}

/** 获取知识库详情 */
export function getKnowledgeBaseById(id: number): Promise<KnowledgeBaseItem> {
  return request.get<KnowledgeBaseItem>(`knowledge-bases/${id}`).then((data) => (data as unknown) as KnowledgeBaseItem)
}

/** 分页获取知识库收录的文章列表 */
export function getKnowledgeBaseContents(
  id: number,
  params?: { page?: number; pageSize?: number }
): Promise<KnowledgeBaseContentsResponse> {
  return request
    .get<KnowledgeBaseContentsResponse>(`knowledge-bases/${id}/contents`, { params })
    .then((data) => (data as unknown) as KnowledgeBaseContentsResponse)
}

/** 创建知识库 */
export function createKnowledgeBase(body: CreateKnowledgeBaseBody): Promise<KnowledgeBaseItem> {
  return request
    .post<KnowledgeBaseItem>('knowledge-bases', body)
    .then((data) => (data as unknown) as KnowledgeBaseItem)
}

/** 更新知识库 */
export function updateKnowledgeBase(id: number, body: UpdateKnowledgeBaseBody): Promise<KnowledgeBaseItem> {
  return request.patch<KnowledgeBaseItem>(`knowledge-bases/${id}`, body).then((data) => (data as unknown) as KnowledgeBaseItem)
}

/** 删除知识库 */
export function deleteKnowledgeBase(id: number): Promise<void> {
  return request.delete(`knowledge-bases/${id}`)
}

/** 添加文章到知识库 */
export function addContentToKnowledgeBase(kbId: number, contentId: number): Promise<void> {
  return request.post(`knowledge-bases/${kbId}/contents`, { contentId })
}

/** 从知识库移除文章 */
export function removeContentFromKnowledgeBase(kbId: number, contentId: number): Promise<void> {
  return request.delete(`knowledge-bases/${kbId}/contents`, { params: { contentId } })
}

/** 订阅知识库 */
export function subscribeKnowledgeBase(id: number): Promise<void> {
  return request.post(`knowledge-bases/${id}/subscribe`)
}

/** 取消订阅知识库 */
export function unsubscribeKnowledgeBase(id: number): Promise<void> {
  return request.delete(`knowledge-bases/${id}/subscribe`)
}
