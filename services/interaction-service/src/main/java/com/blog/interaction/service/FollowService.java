package com.blog.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.interaction.dto.FollowStatsVO;
import com.blog.interaction.entity.Follow;
import com.blog.interaction.mapper.FollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;

    /**
     * 关注了：follower_id = userId 的记录数
     * 关注者：followee_id = userId 的记录数
     * 昨日粉丝增长：followee_id = userId 且 created_at 在昨日 0 点至今日 0 点
     */
    public FollowStatsVO getFollowStats(Long userId) {
        Long following = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId));
        Long followers = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFolloweeId, userId));
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long yesterdayFollowers = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFolloweeId, userId)
                        .ge(Follow::getCreatedAt, yesterdayStart)
                        .lt(Follow::getCreatedAt, todayStart));
        FollowStatsVO vo = new FollowStatsVO();
        vo.setFollowingCount(following != null ? following : 0L);
        vo.setFollowerCount(followers != null ? followers : 0L);
        vo.setYesterdayFollowerDelta(yesterdayFollowers != null ? yesterdayFollowers : 0L);
        return vo;
    }

    /** 当前用户关注的用户 ID 列表（按关注时间倒序），用于关注页横向列表 */
    public List<Long> listFolloweeIds(Long followerId) {
        if (followerId == null) return List.of();
        List<Follow> list = followMapper.selectList(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, followerId)
                        .orderByDesc(Follow::getCreatedAt));
        return list.stream().map(Follow::getFolloweeId).collect(Collectors.toList());
    }

    /** 推荐关注：按粉丝数降序返回用户 ID，排除当前用户，用于侧栏推荐关注列表 */
    public List<Long> listRecommendedUserIds(int limit, Long excludeUserId) {
        if (limit <= 0) return List.of();
        List<Long> ids = followMapper.selectFolloweeIdsByFollowerCountDesc(limit + (excludeUserId != null ? 1 : 0));
        if (excludeUserId != null) {
            ids = ids.stream().filter(id -> !excludeUserId.equals(id)).limit(limit).collect(Collectors.toList());
        }
        return ids;
    }

    /** 当前用户是否已关注指定用户（用于 visibility=FANS 的博客可见性校验） */
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) return false;
        if (followerId.equals(followeeId)) return true;
        Long n = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, followerId)
                        .eq(Follow::getFolloweeId, followeeId));
        return n != null && n > 0;
    }

    /** 指定用户的关注统计（公开），用于他人个人主页展示关注者/关注了数量 */
    public FollowStatsVO getFollowStatsByUserId(Long targetUserId) {
        if (targetUserId == null) {
            FollowStatsVO vo = new FollowStatsVO();
            vo.setFollowingCount(0L);
            vo.setFollowerCount(0L);
            vo.setYesterdayFollowerDelta(0L);
            return vo;
        }
        return getFollowStats(targetUserId);
    }

    /** 关注某用户（不能关注自己） */
    public void follow(Long currentUserId, Long followeeId) {
        if (currentUserId == null || followeeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数不能为空");
        }
        if (currentUserId.equals(followeeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能关注自己");
        }
        Long n = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, currentUserId)
                        .eq(Follow::getFolloweeId, followeeId));
        if (n != null && n > 0) return;
        Follow f = new Follow();
        f.setFollowerId(currentUserId);
        f.setFolloweeId(followeeId);
        f.setCreatedAt(LocalDateTime.now());
        followMapper.insert(f);
    }

    /** 取消关注 */
    public void unfollow(Long currentUserId, Long followeeId) {
        if (currentUserId == null || followeeId == null) return;
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, currentUserId)
                .eq(Follow::getFolloweeId, followeeId));
    }
}
