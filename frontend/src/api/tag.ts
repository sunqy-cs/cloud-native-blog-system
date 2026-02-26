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
