package com.blog.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 分页查询公开知识库，按订阅数降序，支持按名称/简介模糊搜索。
     */
    IPage<KnowledgeBase> selectPublicPopular(IPage<KnowledgeBase> page, @Param("q") String q);
}
