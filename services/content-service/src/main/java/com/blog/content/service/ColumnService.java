package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.content.dto.ColumnVO;
import com.blog.content.dto.CreateColumnRequest;
import com.blog.content.dto.ModerationSubmitRequest;
import com.blog.content.dto.UpdateColumnRequest;
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
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ColumnMapper columnMapper;
    private final ContentMapper contentMapper;
    private final ModerationService moderationService;

    public List<ColumnVO> listMyColumns(Long userId) {
        return listColumnsByUserId(userId);
    }

    /** 按专栏名称或描述模糊搜索（公开），用于搜索页「专栏」；返回带 userId 的 VO，限制 20 条 */
    public List<ColumnVO> searchByName(String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        String keyword = "%" + q.trim() + "%";
        LambdaQueryWrapper<Column> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.like(Column::getName, keyword).or().like(Column::getDescription, keyword))
                .orderByDesc(Column::getUpdatedAt)
                .last("LIMIT 20");
        List<Column> list = columnMapper.selectList(qw);
        return list.stream()
                .map(this::toVOWithUserId)
                .collect(Collectors.toList());
    }

    /** 按 ID 获取专栏详情（公开），用于专栏详情页；返回带 userId 便于跳转博客 */
    public ColumnVO getById(Long id) {
        Column column = columnMapper.selectById(id);
        if (column == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "专栏不存在");
        }
        return toVOWithUserId(column);
    }

    /** 按用户 ID 获取专栏列表（公开），用于他人博客页「全部 / 专栏」导航，无需认证 */
    public List<ColumnVO> listColumnsByUserId(Long userId) {
        if (userId == null) return List.of();
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
        submitColumnModeration(column);
        Column saved = columnMapper.selectById(column.getId());
        return toVO(saved != null ? saved : column);
    }

    public ColumnVO updateColumn(Long userId, Long columnId, UpdateColumnRequest request) {
        Column column = columnMapper.selectById(columnId);
        if (column == null || !userId.equals(column.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "专栏不存在");
        }
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "专栏名称不能为空");
            }
            column.setName(name);
        }
        if (request.getDescription() != null) {
            column.setDescription(request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
        }
        if (request.getCover() != null) {
            column.setCover(request.getCover().trim().isEmpty() ? null : request.getCover().trim());
        }
        columnMapper.updateById(column);
        submitColumnModeration(column);
        Column updated = columnMapper.selectById(columnId);
        return toVO(updated != null ? updated : column);
    }

    public void deleteColumn(Long userId, Long columnId) {
        Column column = columnMapper.selectById(columnId);
        if (column == null || !userId.equals(column.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "专栏不存在");
        }
        contentMapper.update(null, new LambdaUpdateWrapper<Content>()
                .eq(Content::getColumnId, columnId)
                .set(Content::getColumnId, null));
        columnMapper.deleteById(columnId);
    }

    /** 将已发布文章加入专栏（仅本人的文章） */
    public void addContentToColumn(Long userId, Long columnId, Long contentId) {
        Column column = columnMapper.selectById(columnId);
        if (column == null || !userId.equals(column.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "专栏不存在");
        }
        Content content = contentMapper.selectById(contentId);
        if (content == null || !userId.equals(content.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        if (!TYPE_BLOG.equals(content.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可添加博客文章");
        }
        if (!STATUS_PUBLISHED.equals(content.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可添加已发布的文章");
        }
        content.setColumnId(columnId);
        contentMapper.updateById(content);
    }

    /** 从专栏移除文章（仅本人的专栏与文章） */
    public void removeContentFromColumn(Long userId, Long columnId, Long contentId) {
        Column column = columnMapper.selectById(columnId);
        if (column == null || !userId.equals(column.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "专栏不存在");
        }
        Content content = contentMapper.selectById(contentId);
        if (content == null || !userId.equals(content.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        if (!columnId.equals(content.getColumnId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该文章不在本专栏中");
        }
        content.setColumnId(null);
        contentMapper.updateById(content);
    }

    private ColumnVO toVOWithUserId(Column c) {
        ColumnVO vo = toVO(c);
        vo.setUserId(c.getUserId());
        return vo;
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

    private void submitColumnModeration(Column column) {
        if (column == null || column.getId() == null || column.getUserId() == null) return;
        try {
            ModerationSubmitRequest req = new ModerationSubmitRequest();
            req.setResourceType("COLUMN");
            req.setResourceId(column.getId());
            req.setOwnerUserId(column.getUserId());
            req.setPayloadSnapshot(buildColumnModerationPayload(column));
            moderationService.submitTask(req);
        } catch (Exception ignored) {
            // 专栏审核失败不阻塞主流程
        }
    }

    private String buildColumnModerationPayload(Column column) {
        String desc = column.getDescription() != null ? column.getDescription().trim() : "";
        if (desc.length() > 1000) desc = desc.substring(0, 1000);
        return "title=" + (column.getName() != null ? column.getName() : "")
                + "\nsummary=" + desc
                + "\ncover=" + (column.getCover() != null ? column.getCover() : "");
    }
}
