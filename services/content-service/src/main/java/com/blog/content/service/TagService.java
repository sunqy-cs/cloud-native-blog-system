package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.dto.TagVO;
import com.blog.content.entity.Tag;
import com.blog.content.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public List<TagVO> listMainTags() {
        List<Tag> list = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().eq(Tag::getIsMain, 1).orderByAsc(Tag::getId));
        return list.stream().map(t -> {
            TagVO vo = new TagVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            return vo;
        }).collect(Collectors.toList());
    }
}
