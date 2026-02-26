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
        return toVOList(list);
    }

    /** 获取全部非主标签（is_main=0），用于创作页「其他标签」多选 */
    public List<TagVO> listOtherTags() {
        List<Tag> list = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().eq(Tag::getIsMain, 0).orderByAsc(Tag::getId));
        return toVOList(list);
    }

    private List<TagVO> toVOList(List<Tag> list) {
        return list.stream().map(t -> {
            TagVO vo = new TagVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            return vo;
        }).collect(Collectors.toList());
    }
}
