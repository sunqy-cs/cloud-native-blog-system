package com.blog.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.interaction.dto.ContentLikeItemVO;
import com.blog.interaction.dto.ContentLikesMeResponse;
import com.blog.interaction.entity.Content;
import com.blog.interaction.entity.ContentLike;
import com.blog.interaction.mapper.ContentLikeMapper;
import com.blog.interaction.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentLikeService {

    private static final DateTimeFormatter LIKE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ContentLikeMapper contentLikeMapper;
    private final ContentMapper contentMapper;

    public ContentLikesMeResponse listMyLiked(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<ContentLike> q = new LambdaQueryWrapper<>();
        q.eq(ContentLike::getUserId, userId)
                .orderByDesc(ContentLike::getCreatedAt);
        Page<ContentLike> p = contentLikeMapper.selectPage(new Page<>(page, pageSize), q);
        List<ContentLikeItemVO> list = p.getRecords().stream()
                .map(this::toItemVO)
                .collect(Collectors.toList());
        ContentLikesMeResponse res = new ContentLikesMeResponse();
        res.setList(list);
        res.setTotal(p.getTotal());
        return res;
    }

    /**
     * 统计指定内容在昨日 0 点至今日 0 点的新增点赞数（用于创作者中心昨日点赞增长）
     */
    public long countYesterdayByContentIds(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return 0L;
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long count = contentLikeMapper.selectCount(
                new LambdaQueryWrapper<ContentLike>()
                        .in(ContentLike::getContentId, contentIds)
                        .ge(ContentLike::getCreatedAt, yesterdayStart)
                        .lt(ContentLike::getCreatedAt, todayStart));
        return count != null ? count : 0L;
    }

    /** 当前用户是否已点赞该内容（userId 为空时返回 false） */
    public boolean hasLiked(Long userId, Long contentId) {
        if (userId == null || contentId == null) return false;
        Long n = contentLikeMapper.selectCount(
                new LambdaQueryWrapper<ContentLike>()
                        .eq(ContentLike::getUserId, userId)
                        .eq(ContentLike::getContentId, contentId));
        return n != null && n > 0;
    }

    /** 点赞文章（幂等：已赞则不再插入，并同步 content.like_count） */
    @Transactional
    public void like(Long userId, Long contentId) {
        if (userId == null || contentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户或内容 ID 不能为空");
        }
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在");
        }
        LambdaQueryWrapper<ContentLike> q = new LambdaQueryWrapper<>();
        q.eq(ContentLike::getUserId, userId).eq(ContentLike::getContentId, contentId);
        if (contentLikeMapper.selectCount(q) > 0) return;
        ContentLike like = new ContentLike();
        like.setUserId(userId);
        like.setContentId(contentId);
        like.setCreatedAt(LocalDateTime.now());
        contentLikeMapper.insert(like);
        LambdaUpdateWrapper<Content> u = new LambdaUpdateWrapper<>();
        u.eq(Content::getId, contentId).setSql("like_count = like_count + 1");
        contentMapper.update(null, u);
    }

    /** 取消点赞（幂等），并同步 content.like_count（不低于 0） */
    @Transactional
    public void unlike(Long userId, Long contentId) {
        if (userId == null || contentId == null) return;
        LambdaQueryWrapper<ContentLike> q = new LambdaQueryWrapper<>();
        q.eq(ContentLike::getUserId, userId).eq(ContentLike::getContentId, contentId);
        int deleted = contentLikeMapper.delete(q);
        if (deleted > 0) {
            LambdaUpdateWrapper<Content> u = new LambdaUpdateWrapper<>();
            u.eq(Content::getId, contentId).setSql("like_count = GREATEST(0, like_count - 1)");
            contentMapper.update(null, u);
        }
    }

    private ContentLikeItemVO toItemVO(ContentLike like) {
        ContentLikeItemVO vo = new ContentLikeItemVO();
        vo.setContentId(like.getContentId());
        vo.setLikedAt(like.getCreatedAt() != null ? like.getCreatedAt().format(LIKE_TIME_FORMAT) : null);
        return vo;
    }
}
