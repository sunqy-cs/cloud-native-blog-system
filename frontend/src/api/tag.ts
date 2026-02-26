import request from './request'

export interface TagItem {
  id: number
  name: string
}

/**
 * 获取主标签列表（12 个），创作页必选其一
 */
export function getMainTags(): Promise<TagItem[]> {
  return request.get('tags/main').then((data) => (Array.isArray(data) ? data : []) as unknown as TagItem[])
}

/**
 * 获取非主标签列表，创作页「其他标签」从已有标签中多选
 */
export function getOtherTags(): Promise<TagItem[]> {
  return request.get('tags/others').then((data) => (Array.isArray(data) ? data : []) as unknown as TagItem[])
}
