import request from './request'

export interface UserMessageItem {
  id: number
  senderUserId?: number | null
  title: string
  body?: string
  msgType: string
  scene?: string
  extra?: string
  read: boolean
  readAt?: string | null
  createdAt: string
}

export interface UserMessagePage {
  records: UserMessageItem[]
  total: number
}

export function listUserMessages(params: { page?: number; pageSize?: number; unreadOnly?: boolean }) {
  return request.get<UserMessagePage>('users/me/messages', { params })
}

export function getUnreadMessageCount() {
  return request.get<{ count: number }>('users/me/messages/unread-count')
}

export function markUserMessageRead(id: number) {
  return request.patch(`users/me/messages/${id}/read`)
}

export function markAllMessagesRead() {
  return request.patch('users/me/messages/read-all')
}
