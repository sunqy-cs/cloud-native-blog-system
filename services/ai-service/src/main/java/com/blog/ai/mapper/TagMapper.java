package com.blog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.ai.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
