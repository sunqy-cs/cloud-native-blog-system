package com.blog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.interaction.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    /** 按关注人数（粉丝数）降序返回用户 ID，用于推荐关注 */
    @Select("SELECT followee_id FROM follow GROUP BY followee_id ORDER BY COUNT(*) DESC LIMIT #{limit}")
    List<Long> selectFolloweeIdsByFollowerCountDesc(@Param("limit") int limit);
}
