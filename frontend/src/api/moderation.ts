import request from './request'

/** 与表 moderation_task.resource_type 一致 */
export type ModerationResourceType =
  | 'ARTICLE'
  | 'KNOWLEDGE_DOC'
  | 'COMMENT'
  | 'USER_PROFILE'
  | 'COLUMN'
  | 'KNOWLEDGE_BASE'

/** 与表 moderation_task.status 一致 */
export type ModerationTaskStatus = 'PENDING' | 'NEEDS_HUMAN' | 'APPROVED' | 'REJECTED'

export interface ModerationTask {
  id: number
  resourceType: ModerationResourceType
  resourceId: number
  resourceTitle?: string
  ownerUserId: number
  ownerUsername?: string
  ownerAvatar?: string
  status: ModerationTaskStatus
  aiDecision?: string | null
  aiDetail?: Record<string, unknown> | null
  aiReviewedAt?: string | null
  humanReviewerId?: number | null
  humanReviewerName?: string | null
  humanDecision?: string | null
  humanNote?: string | null
  humanReviewedAt?: string | null
  payloadSnapshot?: string | null
  createdAt: string
  updatedAt: string
}

export interface ModerationTaskPage {
  records: ModerationTask[]
  total: number
}

export interface ListModerationParams {
  page?: number
  pageSize?: number
  resourceType?: ModerationResourceType
  status?: string
  finishedOnly?: boolean
}

export interface ModerationStats {
  pending: number
  pendingHuman: number
  todayFinished: number
  rejected7d: number
}

const RESOURCE_LABEL: Record<ModerationResourceType, string> = {
  ARTICLE: '博客文章',
  KNOWLEDGE_DOC: '知识库文档',
  COMMENT: '评论',
  USER_PROFILE: '个人资料',
  COLUMN: '专栏',
  KNOWLEDGE_BASE: '知识库',
}

export function moderationResourceLabel(t: ModerationResourceType): string {
  return RESOURCE_LABEL[t] ?? t
}

export function moderationStatusLabel(s: string): string {
  const m: Record<string, string> = {
    PENDING: '待 AI',
    NEEDS_HUMAN: '待人工',
    APPROVED: '已通过',
    REJECTED: '已驳回',
  }
  return m[s] ?? s
}

/**
 * 列表。对接后端后改为：
 * return request.get<ModerationTaskPage>('admin/moderation/tasks', { params })
 */
export async function listModerationTasks(params: ListModerationParams): Promise<ModerationTaskPage> {
  return request.get<ModerationTaskPage>('admin/moderation/tasks', {
    params: {
      page: params.page ?? 1,
      pageSize: params.pageSize ?? 20,
      resourceType: params.resourceType,
      status: params.status,
      finishedOnly: params.finishedOnly ?? false,
    },
  })
}

/**
 * 首页统计。对接后端后改为：
 * return request.get<ModerationStats>('admin/moderation/stats')
 */
export async function getModerationStats(): Promise<ModerationStats> {
  return request.get<ModerationStats>('admin/moderation/stats')
}

export async function humanReviewModerationTask(id: number, decision: 'APPROVE' | 'REJECT', note = '') {
  return request.post<ModerationTask>(`admin/moderation/tasks/${id}/human-review`, { decision, note })
}

export async function rerunAiReview(id: number) {
  return request.post<ModerationTask>(`admin/moderation/tasks/${id}/ai-review`)
}
