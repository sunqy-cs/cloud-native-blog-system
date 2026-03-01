import request from './request'

export interface FollowStats {
  followingCount: number
  followerCount: number
  /** 昨日新增粉丝数 */
  yesterdayFollowerDelta?: number
}

/**
 * 获取当前用户关注统计（关注了、关注者数量），用于个人页右侧展示
 */
export function getFollowMe(): Promise<FollowStats> {
  return request.get<FollowStats>('follow/me').then((data) => data as FollowStats)
}

/**
 * 获取指定用户的关注统计（公开），用于他人个人主页展示
 */
export function getFollowStatsByUserId(userId: number): Promise<FollowStats> {
  return request.get<FollowStats>('follow/stats', { params: { userId } }).then((data) => data as FollowStats)
}

/**
 * 当前用户是否已关注指定用户（用于展示「已关注」按钮）
 */
export function checkFollow(followeeId: number): Promise<{ following: boolean }> {
  return request.get<{ following: boolean }>('follow/check', { params: { followeeId } }).then((data) => data as { following: boolean })
}

/**
 * 关注指定用户
 */
export function followUser(followeeId: number): Promise<void> {
  return request.post('follow', { followeeId })
}

/**
 * 取消关注指定用户
 */
export function unfollowUser(followeeId: number): Promise<void> {
  return request.delete(`follow/${followeeId}`)
}
