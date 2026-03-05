package com.blog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.interaction.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    /** 按关注人数（粉丝数）降序返回用户 ID，用于推荐关注（仅包含至少 1 个粉丝的用户） */
    @Select("SELECT followee_id FROM follow GROUP BY followee_id ORDER BY COUNT(*) DESC LIMIT #{limit}")
    List<Long> selectFolloweeIdsByFollowerCountDesc(@Param("limit") int limit);

    /** 影响力榜：从 user 表左连粉丝数，含粉丝数为 0 的用户，按粉丝数降序 */
    @Select("SELECT u.id FROM `user` u LEFT JOIN (SELECT followee_id, COUNT(*) AS cnt FROM follow GROUP BY followee_id) f ON u.id = f.followee_id ORDER BY COALESCE(f.cnt, 0) DESC LIMIT #{limit}")
    List<Long> selectAllUserIdsByFollowerCountDesc(@Param("limit") int limit);

    /** 近期涨粉榜：按近 7 天新增粉丝数降序返回用户 ID（仅包含 7 天内有涨粉的用户） */
    @Select("SELECT followee_id FROM follow WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY followee_id ORDER BY COUNT(*) DESC LIMIT #{limit}")
    List<Long> selectFolloweeIdsByRecentFollowerGrowthDesc(@Param("limit") int limit);

    /** 成长力榜：从 user 表左连近 7 天涨粉数，含涨粉为 0 的用户，按涨粉数降序 */
    @Select("SELECT u.id FROM `user` u LEFT JOIN (SELECT followee_id, COUNT(*) AS cnt FROM follow WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY followee_id) f ON u.id = f.followee_id ORDER BY COALESCE(f.cnt, 0) DESC LIMIT #{limit}")
    List<Long> selectAllUserIdsByRecentFollowerGrowthDesc(@Param("limit") int limit);
}
