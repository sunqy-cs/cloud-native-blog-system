package com.blog.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.user.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {
}
