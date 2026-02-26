package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.dto.ColumnVO;
import com.blog.content.dto.CreateColumnRequest;
import com.blog.content.entity.Column;
import com.blog.content.entity.Content;
import com.blog.content.mapper.ColumnMapper;
import com.blog.content.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColumnService {

    private static final String TYPE_BLOG = "BLOG";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ColumnMapper columnMapper;
    private final ContentMapper contentMapper;

    public List<ColumnVO> listMyColumns(Long userId) {
        LambdaQueryWrapper<Column> q = new LambdaQueryWrapper<>();
        q.eq(Column::getUserId, userId)
                .orderByDesc(Column::getUpdatedAt)
                .orderByDesc(Column::getCreatedAt);
        List<Column> list = columnMapper.selectList(q);
        return list.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public ColumnVO createColumn(Long userId, CreateColumnRequest request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "专栏名称不能为空");
        }
        Column column = new Column();
        column.setUserId(userId);
        column.setName(name);
        column.setDescription(request.getDescription() != null && !request.getDescription().trim().isEmpty()
                ? request.getDescription().trim() : null);
        column.setCover(request.getCover() != null && !request.getCover().trim().isEmpty()
                ? request.getCover().trim() : null);
        columnMapper.insert(column);
        Column saved = columnMapper.selectById(column.getId());
        return toVO(saved != null ? saved : column);
    }

    private ColumnVO toVO(Column c) {
        ColumnVO vo = new ColumnVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setDescription(c.getDescription());
        vo.setCover(c.getCover());
        vo.setArticleCount(countByColumnId(c.getId()));
        vo.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FORMAT) : null);
        vo.setUpdatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().format(DATE_FORMAT) : null);
        return vo;
    }

    private int countByColumnId(Long columnId) {
        if (columnId == null) return 0;
        Long n = contentMapper.selectCount(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getColumnId, columnId)
                        .eq(Content::getType, TYPE_BLOG));
        return n != null ? n.intValue() : 0;
    }
}
