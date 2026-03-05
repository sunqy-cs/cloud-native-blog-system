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

/**
 * 一键生成：根据 bot 与 prompt 生成正文、标题、摘要、小标签、封面、主标签
 */
export interface OneClickGenerateResult {
  body: string
  title: string
  summary: string
  tagNames: string[]
  coverUrl: string | null
  mainTagId: number | null
}

export function oneClickGenerate(botId: number, prompt: string): Promise<OneClickGenerateResult> {
  return request
    .post('ai/one-click-generate', { botId, prompt }, { timeout: 200000 })
    .then((data) => (data as unknown) as OneClickGenerateResult)
}

/** RAG 会话列表（需传 userId 用于 X-User-Id） */
export interface RagConversationItem {
  id: number
  userId: number
  kbId: number | null
  title: string | null
  createdAt: string
  updatedAt: string
}

export function getRagConversations(userId: number, limit = 30): Promise<RagConversationItem[]> {
  return request
    .get<RagConversationItem[]>('rag/conversations', {
      params: { limit },
      headers: { 'X-User-Id': String(userId) },
    })
    .then((data) => (Array.isArray(data) ? data : []))
}

/** RAG 某会话的消息列表 */
export interface RagMessageItem {
  id: number
  conversationId: number
  role: string
  content: string
  createdAt: string
}

export function getRagConversationMessages(
  userId: number,
  conversationId: number,
  limit = 100
): Promise<RagMessageItem[]> {
  return request
    .get<RagMessageItem[]>(`rag/conversations/${conversationId}/messages`, {
      params: { limit },
      headers: { 'X-User-Id': String(userId) },
    })
    .then((data) => (Array.isArray(data) ? data : []))
}

/** 删除 RAG 会话（仅本人；关联消息由后端 CASCADE 删除） */
export function deleteRagConversation(userId: number, conversationId: number): Promise<void> {
  return request.delete(`rag/conversations/${conversationId}`, {
    headers: { 'X-User-Id': String(userId) },
  }) as Promise<void>
}

/** RAG 流式对话：SSE，onChunk 每段文本，onDone 结束，onError 失败 */
export async function ragChatStream(
  params: { conversationId?: number | null; kbIds?: number[]; question: string },
  callbacks: { onChunk: (text: string) => void; onDone: () => void; onError: (err: string) => void }
) {
  const baseURL = import.meta.env.VITE_API_BASE ?? '/api'
  const url = `${baseURL.replace(/\/$/, '')}/rag/chat`
  const { useUserStore } = await import('@/stores/user')
  const userStore = useUserStore()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
    'X-User-Id': String(userStore.userInfo?.id ?? ''),
  }
  if (userStore.token) headers['Authorization'] = `Bearer ${userStore.token}`

  try {
    const res = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        conversationId: params.conversationId ?? null,
        kbIds: params.kbIds ?? [],
        question: params.question,
      }),
    })
    if (!res.ok) {
      const text = await res.text()
      callbacks.onError(text || `HTTP ${res.status}`)
      return
    }
    const reader = res.body?.getReader()
    if (!reader) {
      callbacks.onError('无响应流')
      return
    }
    const dec = new TextDecoder()
    let buf = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += dec.decode(value, { stream: true })
      const lines = buf.split('\n')
      buf = lines.pop() ?? ''
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(line.indexOf(':') + 1).trim()
          if (data === '[DONE]' || data === '') continue
          try {
            if (data) callbacks.onChunk(data)
          } catch (_) {}
        }
      }
    }
    callbacks.onDone()
  } catch (e) {
    callbacks.onError(e instanceof Error ? e.message : String(e))
  }
}
