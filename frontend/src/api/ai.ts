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

/**
 * 根据正文生成标签（AI），最多 5 个标签名
 */
export function generateTags(body: string): Promise<{ tagNames: string[] }> {
  return request.post('ai/tags', { body }).then((data) => data as unknown as { tagNames: string[] })
}

/**
 * 根据正文生成封面图（AI）：先生成封面描述再文生图，返回封面 URL
 */
export function generateCover(body: string): Promise<{ url: string }> {
  return request.post('ai/cover', { body }).then((data) => data as unknown as { url: string })
}
