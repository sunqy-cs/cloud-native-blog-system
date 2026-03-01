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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.blog.interaction.dto.CreateCommentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String TYPE_BLOG = "BLOG";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CommentMapper commentMapper;
    private final ContentMapper contentMapper;

    @Autowired
    @Qualifier("plainRestTemplate")
    private RestTemplate restTemplate;

    @Value("${app.user-service-url:http://localhost:8081}")
    private String userServiceBaseUrl;

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
     * 某篇文章的评论列表。已发布的博客对所有人可见；未发布或非博客则仅作者可见。
     */
    public List<CommentVO> listByContentId(Long contentId, Long currentUserId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            return List.of();
        }
        boolean isPublishedBlog = TYPE_BLOG.equals(content.getType()) && STATUS_PUBLISHED.equals(content.getStatus());
        boolean isAuthor = content.getUserId() != null && content.getUserId().equals(currentUserId);
        if (!isPublishedBlog && !isAuthor) {
            return List.of();
        }
        Long contentOwnerId = content.getUserId();
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<>();
        q.eq(Comment::getContentId, contentId)
                .orderByDesc(Comment::getIsHot)
                .orderByDesc(Comment::getCreatedAt);
        List<Comment> list = commentMapper.selectList(q);
        return list.stream()
                .map(c -> toCommentVO(c, contentOwnerId, getNicknameAndAvatar(c.getUserId())))
                .collect(Collectors.toList());
    }

    /**
     * 发表评论或回复。仅允许对已发布的博客评论；parentId 可选，表示回复某条评论。
     */
    @Transactional
    public CommentVO create(Long currentUserId, CreateCommentRequest req) {
        String body = req.getBody() != null ? req.getBody().trim() : "";
        if (body.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        Content content = contentMapper.selectById(req.getContentId());
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在");
        }
        if (!TYPE_BLOG.equals(content.getType()) || !STATUS_PUBLISHED.equals(content.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持对已发布的博客进行评论");
        }
        Long parentId = req.getParentId();
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getContentId().equals(req.getContentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "父评论不存在或不属于本文章");
            }
        }
        Comment comment = new Comment();
        comment.setUserId(currentUserId);
        comment.setContentId(req.getContentId());
        comment.setBody(body);
        comment.setParentId(parentId);
        comment.setIsHot(false);
        commentMapper.insert(comment);
        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(LocalDateTime.now());
        }
        LambdaUpdateWrapper<Content> updateContent = new LambdaUpdateWrapper<>();
        updateContent.eq(Content::getId, req.getContentId())
                .setSql("comment_count = comment_count + 1");
        contentMapper.update(null, updateContent);
        return toCommentVO(comment, content.getUserId(), getNicknameAndAvatar(currentUserId));
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

    /** 从 user-service 拉取昵称与头像，失败时返回兜底昵称 */
    private NicknameAvatar getNicknameAndAvatar(Long userId) {
        if (userId == null) return new NicknameAvatar("用户", null);
        try {
            String url = userServiceBaseUrl.replaceFirst("/$", "") + "/api/users/" + userId;
            @SuppressWarnings("unchecked")
            var map = restTemplate.getForObject(url, java.util.Map.class);
            if (map == null) return new NicknameAvatar("用户" + userId, null);
            String nickname = (String) map.get("nickname");
            String username = (String) map.get("username");
            String avatar = (String) map.get("avatar");
            String displayName = (nickname != null && !nickname.isEmpty()) ? nickname : (username != null ? username : "用户" + userId);
            return new NicknameAvatar(displayName, avatar);
        } catch (Exception e) {
            return new NicknameAvatar("用户" + userId, null);
        }
    }

    private CommentVO toCommentVO(Comment c, Long contentOwnerId, NicknameAvatar user) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setUserId(c.getUserId());
        vo.setUserNickname(user != null ? user.nickname : ("用户" + c.getUserId()));
        vo.setUserAvatar(user != null ? user.avatar : null);
        vo.setContentId(c.getContentId());
        vo.setBody(c.getBody());
        vo.setParentId(c.getParentId());
        vo.setIsHot(Boolean.TRUE.equals(c.getIsHot()));
        vo.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DATETIME_FORMAT) : null);
        vo.setIsAuthor(contentOwnerId != null && contentOwnerId.equals(c.getUserId()));
        return vo;
    }

    private record NicknameAvatar(String nickname, String avatar) {}
}
