import request from './request'

/**
 * 根据正文生成博客标题（AI）
 */
export function generateTitle(body: string): Promise<{ title: string }> {
  return request.post('ai/title', { body }).then((data) => data as unknown as { title: string })
}

/**
 * 根据正文生成文章摘要（AI），上限 100 字
 */
export function generateSummary(body: string): Promise<{ summary: string }> {
  return request.post('ai/summary', { body }).then((data) => data as unknown as { summary: string })
}
