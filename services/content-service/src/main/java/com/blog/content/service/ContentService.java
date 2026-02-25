package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.dto.ContentListItemVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.entity.Content;
import com.blog.content.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentService {

    private static final String TYPE_BLOG = "BLOG";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ContentMapper contentMapper;

    public ContentsMeResponse listMyContents(Long userId, int page, int pageSize,
                                            String visibility, String sortBy, String order) {
        LambdaQueryWrapper<Content> q = new LambdaQueryWrapper<>();
        q.eq(Content::getUserId, userId)
                .eq(Content::getType, TYPE_BLOG);
        if (visibility != null && !visibility.isBlank()) {
            q.eq(Content::getVisibility, visibility.toUpperCase());
        }
        boolean asc = "asc".equalsIgnoreCase(order);
        if ("likes".equalsIgnoreCase(sortBy)) {
            q.orderBy(true, asc, Content::getLikeCount);
            q.orderBy(true, false, Content::getCreatedAt);
        } else if ("views".equalsIgnoreCase(sortBy)) {
            q.orderBy(true, asc, Content::getViewCount);
            q.orderBy(true, false, Content::getCreatedAt);
        } else {
            q.orderBy(true, asc, Content::getCreatedAt);
        }
        Page<Content> p = contentMapper.selectPage(new Page<>(page, pageSize), q);
        List<ContentListItemVO> list = p.getRecords().stream()
                .map(this::toListItemVO)
                .collect(Collectors.toList());
        ContentsMeResponse res = new ContentsMeResponse();
        res.setList(list);
        res.setTotal(p.getTotal());
        return res;
    }

    /**
     * 按 ID 列表批量返回内容摘要，顺序与请求 ids 一致；不存在的 ID 不返回。
     */
    public List<ContentListItemVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<Content> list = contentMapper.selectBatchIds(ids);
        if (list.isEmpty()) return Collections.emptyList();
        List<ContentListItemVO> voList = list.stream().map(this::toListItemVO).collect(Collectors.toList());
        // 按请求 ids 顺序排列
        List<ContentListItemVO> ordered = new ArrayList<>(voList.size());
        for (Long id : ids) {
            voList.stream().filter(vo -> id.equals(vo.getId())).findFirst().ifPresent(ordered::add);
        }
        return ordered;
    }

    private ContentListItemVO toListItemVO(Content c) {
        ContentListItemVO vo = new ContentListItemVO();
        vo.setId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setSummary(c.getSummary());
        vo.setCover(c.getCover());
        vo.setViewCount(c.getViewCount() != null ? c.getViewCount() : 0);
        vo.setLikeCount(c.getLikeCount() != null ? c.getLikeCount() : 0);
        vo.setCollectionCount(c.getCollectionCount() != null ? c.getCollectionCount() : 0);
        vo.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FORMAT) : null);
        return vo;
    }
}
