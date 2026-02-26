import request from './request'

/**
 * 根据正文生成博客标题（AI）
 */
export function generateTitle(body: string): Promise<{ title: string }> {
  return request.post<{ title: string }>('ai/title', { body }).then((data) => data as { title: string })
}
