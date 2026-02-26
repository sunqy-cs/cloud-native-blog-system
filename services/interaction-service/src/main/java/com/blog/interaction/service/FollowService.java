package com.blog.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.interaction.dto.FollowStatsVO;
import com.blog.interaction.entity.Follow;
import com.blog.interaction.mapper.FollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
