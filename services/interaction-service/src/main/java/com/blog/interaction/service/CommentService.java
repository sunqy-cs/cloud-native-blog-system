package com.blog.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.interaction.dto.CommentedArticleVO;
import com.blog.interaction.dto.CommentVO;
import com.blog.interaction.entity.Comment;
import com.blog.interaction.entity.Content;
import com.blog.interaction.mapper.CommentMapper;
import com.blog.interaction.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String TYPE_BLOG = "BLOG";
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CommentMapper commentMapper;
    private final ContentMapper contentMapper;

    /**
     * 当前用户作为作者、有评论的文章列表（用于评论管理左侧）
     */
    public List<CommentedArticleVO> listCommentedArticles(Long userId) {
        LambdaQueryWrapper<Content> contentQ = new LambdaQueryWrapper<>();
        contentQ.eq(Content::getUserId, userId)
                .eq(Content::getType, TYPE_BLOG)
                .gt(Content::getCommentCount, 0)
                .orderByDesc(Content::getUpdatedAt);
        List<Content> contents = contentMapper.selectList(contentQ);
        List<CommentedArticleVO> result = new ArrayList<>();
        for (Content c : contents) {
            CommentedArticleVO vo = new CommentedArticleVO();
            vo.setContentId(c.getId());
            vo.setTitle(c.getTitle());
            vo.setCommentCount(c.getCommentCount() != null ? c.getCommentCount() : 0);
            vo.setLastCommentAt(c.getUpdatedAt() != null ? c.getUpdatedAt().format(DATETIME_FORMAT) : null);
            result.add(vo);
        }
        return result;
    }

    /**
     * 某篇文章的评论列表（仅允许该文章作者调用）
     */
    public List<CommentVO> listByContentId(Long contentId, Long currentUserId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || !content.getUserId().equals(currentUserId)) {
            return List.of();
        }
        Long contentOwnerId = content.getUserId();
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<>();
        q.eq(Comment::getContentId, contentId)
                .orderByDesc(Comment::getIsHot)
                .orderByDesc(Comment::getCreatedAt);
        List<Comment> list = commentMapper.selectList(q);
        return list.stream().map(c -> toCommentVO(c, contentOwnerId)).collect(Collectors.toList());
    }

    /**
     * 设置/取消热评（仅文章作者可操作）
     */
    @Transactional
    public void setHot(Long commentId, Long currentUserId, boolean hot) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) return;
        Content content = contentMapper.selectById(comment.getContentId());
        if (content == null || !content.getUserId().equals(currentUserId)) return;
        LambdaUpdateWrapper<Comment> u = new LambdaUpdateWrapper<>();
        u.eq(Comment::getId, commentId).set(Comment::getIsHot, hot);
        commentMapper.update(null, u);
    }

    private CommentVO toCommentVO(Comment c, Long contentOwnerId) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setUserId(c.getUserId());
        vo.setUserNickname("用户" + c.getUserId());
        vo.setContentId(c.getContentId());
        vo.setBody(c.getBody());
        vo.setParentId(c.getParentId());
        vo.setIsHot(Boolean.TRUE.equals(c.getIsHot()));
        vo.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DATETIME_FORMAT) : null);
        vo.setIsAuthor(contentOwnerId != null && contentOwnerId.equals(c.getUserId()));
        return vo;
    }
}
