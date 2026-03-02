import request from './request'

export interface CollectionFolderItem {
  id: number
  name: string
  description?: string | null
  isDefault: boolean
  count: number
  createdAt: string
}

export interface CreateFolderBody {
  name: string
  description?: string
}

export interface UpdateFolderBody {
  name?: string
  description?: string
}

/**
 * 获取当前用户的收藏夹列表
 */
export function getCollectionFoldersMe(): Promise<CollectionFolderItem[]> {
  return request.get<CollectionFolderItem[]>('collection-folders/me').then((data) => data as CollectionFolderItem[])
}

/**
 * 创建收藏夹
 */
export function createCollectionFolder(body: CreateFolderBody): Promise<CollectionFolderItem> {
  return request.post<CollectionFolderItem>('collection-folders', body).then((data) => data as CollectionFolderItem)
}

/**
 * 更新收藏夹（名称、简介）。默认收藏夹不可修改。
 */
export function updateCollectionFolder(id: number, body: UpdateFolderBody): Promise<CollectionFolderItem> {
  return request.patch<CollectionFolderItem>(`collection-folders/${id}`, body).then((data) => data as CollectionFolderItem)
}

/**
 * 删除收藏夹
 */
export function deleteCollectionFolder(id: number): Promise<void> {
  return request.delete(`collection-folders/${id}`)
}

/**
 * 获取单个收藏夹详情（当前用户）
 */
export function getCollectionFolderById(id: number): Promise<CollectionFolderItem> {
  return request
    .get<CollectionFolderItem>(`collection-folders/${id}`)
    .then((data) => data as CollectionFolderItem)
}

/** 收藏夹内文章列表分页响应（与 contents/list 一致） */
export interface CollectionFolderContentsResponse {
  list: { id: number; title: string; summary?: string; cover?: string | null; viewCount: number; likeCount: number; collectionCount: number; commentCount?: number; createdAt: string; publishedAt?: string }[]
  total: number
}

/**
 * 分页获取收藏夹内的文章列表
 */
export function getCollectionFolderContents(
  folderId: number,
  params?: { page?: number; pageSize?: number }
): Promise<CollectionFolderContentsResponse> {
  return request
    .get<CollectionFolderContentsResponse>(`collection-folders/${folderId}/contents`, { params })
    .then((data) => data as CollectionFolderContentsResponse)
}

/**
 * 将文章加入收藏夹
 */
export function addContentToCollectionFolder(folderId: number, contentId: number): Promise<void> {
  return request.post(`collection-folders/${folderId}/contents`, { contentId })
}

/**
 * 从收藏夹移除文章
 */
export function removeContentFromCollectionFolder(folderId: number, contentId: number): Promise<void> {
  return request.delete(`collection-folders/${folderId}/contents`, { params: { contentId } })
}

/**
 * 当前用户收藏夹中包含该文章的收藏夹 ID 列表
 */
export function getFolderIdsContainingContent(contentId: number): Promise<number[]> {
  return request
    .get<number[]>(`collection-folders/containing/${contentId}`)
    .then((data) => (Array.isArray(data) ? data : []))
}
