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
