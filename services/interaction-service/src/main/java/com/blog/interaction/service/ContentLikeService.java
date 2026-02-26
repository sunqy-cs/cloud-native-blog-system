package com.blog.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.interaction.dto.ContentLikeItemVO;
import com.blog.interaction.dto.ContentLikesMeResponse;
import com.blog.interaction.entity.ContentLike;
import com.blog.interaction.mapper.ContentLikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private ContentLikeItemVO toItemVO(ContentLike like) {
        ContentLikeItemVO vo = new ContentLikeItemVO();
        vo.setContentId(like.getContentId());
        vo.setLikedAt(like.getCreatedAt() != null ? like.getCreatedAt().format(LIKE_TIME_FORMAT) : null);
        return vo;
    }
}
