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
