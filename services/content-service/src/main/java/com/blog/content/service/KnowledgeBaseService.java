package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.dto.*;
import com.blog.content.entity.*;
import com.blog.content.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private static final String TYPE_BLOG = "BLOG";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String VISIBILITY_PRIVATE = "PRIVATE";
    private static final String VISIBILITY_PUBLIC = "PUBLIC";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseContentMapper knowledgeBaseContentMapper;
    private final KnowledgeBaseFavoriteMapper knowledgeBaseFavoriteMapper;
    private final ContentMapper contentMapper;

    public List<KnowledgeBaseVO> listMy(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> q = new LambdaQueryWrapper<>();
        q.eq(KnowledgeBase::getUserId, userId)
                .orderByDesc(KnowledgeBase::getUpdatedAt)
                .orderByDesc(KnowledgeBase::getCreatedAt);
        List<KnowledgeBase> list = knowledgeBaseMapper.selectList(q);
        return list.stream().map(kb -> toVO(kb)).collect(Collectors.toList());
    }

    public KnowledgeBaseListResponse listPopular(Long userId, int page, int pageSize, String q) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        String searchQ = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        Page<KnowledgeBase> pg = new Page<>(page, pageSize);
        IPage<KnowledgeBase> result = knowledgeBaseMapper.selectPublicPopular(pg, searchQ);
        List<KnowledgeBaseVO> list = result.getRecords().stream().map(kb -> {
            KnowledgeBaseVO vo = toVO(kb);
            if (userId != null) vo.setSubscribed(isSubscribed(userId, kb.getId()));
            return vo;
        }).collect(Collectors.toList());
        KnowledgeBaseListResponse res = new KnowledgeBaseListResponse();
        res.setList(list);
        res.setTotal(result.getTotal());
        return res;
    }

    public List<KnowledgeBaseVO> listSubscribed(Long userId) {
        LambdaQueryWrapper<KnowledgeBaseFavorite> q = new LambdaQueryWrapper<>();
        q.eq(KnowledgeBaseFavorite::getUserId, userId);
        List<KnowledgeBaseFavorite> favs = knowledgeBaseFavoriteMapper.selectList(q);
        if (favs.isEmpty()) return List.of();
        List<Long> kbIds = favs.stream().map(KnowledgeBaseFavorite::getKnowledgeBaseId).distinct().collect(Collectors.toList());
        List<KnowledgeBase> kbs = knowledgeBaseMapper.selectBatchIds(kbIds);
        return kbs.stream()
                .filter(kb -> VISIBILITY_PUBLIC.equals(kb.getVisibility()))
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public KnowledgeBaseVO getById(Long userId, Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        if (VISIBILITY_PRIVATE.equals(kb.getVisibility()) && !kb.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        KnowledgeBaseVO vo = toVO(kb);
        if (userId != null) {
            vo.setSubscribed(isSubscribed(userId, id));
        }
        return vo;
    }

    public KnowledgeBaseContentsResponse listContents(Long userId, Long kbId, int page, int pageSize) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        if (VISIBILITY_PRIVATE.equals(kb.getVisibility()) && !kb.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        long total = knowledgeBaseContentMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseContent>().eq(KnowledgeBaseContent::getKnowledgeBaseId, kbId));
        if (total == 0) {
            KnowledgeBaseContentsResponse empty = new KnowledgeBaseContentsResponse();
            empty.setList(List.of());
            empty.setTotal(0L);
            return empty;
        }
        Page<KnowledgeBaseContent> p = knowledgeBaseContentMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<KnowledgeBaseContent>()
                        .eq(KnowledgeBaseContent::getKnowledgeBaseId, kbId)
                        .orderByDesc(KnowledgeBaseContent::getCreatedAt));
        List<Long> contentIds = p.getRecords().stream().map(KnowledgeBaseContent::getContentId).collect(Collectors.toList());
        List<Content> contents = contentMapper.selectBatchIds(contentIds);
        List<KnowledgeBaseContentItemVO> list = contents.stream()
                .map(this::toContentItemVO)
                .collect(Collectors.toList());
        KnowledgeBaseContentsResponse res = new KnowledgeBaseContentsResponse();
        res.setList(list);
        res.setTotal(total);
        return res;
    }

    @Transactional
    public KnowledgeBaseVO create(Long userId, CreateKnowledgeBaseRequest request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "知识库名称不能为空");
        }
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(userId);
        kb.setName(name);
        kb.setDescription(request.getDescription() != null && !request.getDescription().trim().isEmpty()
                ? request.getDescription().trim() : null);
        kb.setCover(request.getCover() != null && !request.getCover().trim().isEmpty()
                ? request.getCover().trim() : null);
        kb.setVisibility(request.getVisibility() != null && VISIBILITY_PUBLIC.equals(request.getVisibility())
                ? VISIBILITY_PUBLIC : VISIBILITY_PRIVATE);
        knowledgeBaseMapper.insert(kb);
        return toVO(kb);
    }

    @Transactional
    public KnowledgeBaseVO update(Long userId, Long id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "知识库名称不能为空");
            }
            kb.setName(name);
        }
        if (request.getDescription() != null) {
            kb.setDescription(request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
        }
        if (request.getCover() != null) {
            kb.setCover(request.getCover().trim().isEmpty() ? null : request.getCover().trim());
        }
        if (request.getVisibility() != null) {
            kb.setVisibility(VISIBILITY_PUBLIC.equals(request.getVisibility()) ? VISIBILITY_PUBLIC : VISIBILITY_PRIVATE);
        }
        knowledgeBaseMapper.updateById(kb);
        return toVO(knowledgeBaseMapper.selectById(id));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        knowledgeBaseContentMapper.delete(new LambdaQueryWrapper<KnowledgeBaseContent>()
                .eq(KnowledgeBaseContent::getKnowledgeBaseId, id));
        knowledgeBaseFavoriteMapper.delete(new LambdaQueryWrapper<KnowledgeBaseFavorite>()
                .eq(KnowledgeBaseFavorite::getKnowledgeBaseId, id));
        knowledgeBaseMapper.deleteById(id);
    }

    @Transactional
    public void addContent(Long userId, Long kbId, Long contentId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        Content content = contentMapper.selectById(contentId);
        if (content == null || !content.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        if (!TYPE_BLOG.equals(content.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可添加博客文章");
        }
        if (!STATUS_PUBLISHED.equals(content.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可添加已发布的文章");
        }
        long exist = knowledgeBaseContentMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseContent>()
                        .eq(KnowledgeBaseContent::getKnowledgeBaseId, kbId)
                        .eq(KnowledgeBaseContent::getContentId, contentId));
        if (exist > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已收录该文章");
        }
        KnowledgeBaseContent kbc = new KnowledgeBaseContent();
        kbc.setKnowledgeBaseId(kbId);
        kbc.setContentId(contentId);
        knowledgeBaseContentMapper.insert(kbc);
    }

    @Transactional
    public void removeContent(Long userId, Long kbId, Long contentId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        int deleted = knowledgeBaseContentMapper.delete(
                new LambdaQueryWrapper<KnowledgeBaseContent>()
                        .eq(KnowledgeBaseContent::getKnowledgeBaseId, kbId)
                        .eq(KnowledgeBaseContent::getContentId, contentId));
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该文章不在本知识库中");
        }
    }

    @Transactional
    public void subscribe(Long userId, Long kbId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        if (kb.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能订阅自己的知识库");
        }
        if (VISIBILITY_PRIVATE.equals(kb.getVisibility())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能订阅私有知识库");
        }
        long exist = knowledgeBaseFavoriteMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseFavorite>()
                        .eq(KnowledgeBaseFavorite::getUserId, userId)
                        .eq(KnowledgeBaseFavorite::getKnowledgeBaseId, kbId));
        if (exist > 0) return;
        KnowledgeBaseFavorite fav = new KnowledgeBaseFavorite();
        fav.setUserId(userId);
        fav.setKnowledgeBaseId(kbId);
        knowledgeBaseFavoriteMapper.insert(fav);
    }

    @Transactional
    public void unsubscribe(Long userId, Long kbId) {
        knowledgeBaseFavoriteMapper.delete(
                new LambdaQueryWrapper<KnowledgeBaseFavorite>()
                        .eq(KnowledgeBaseFavorite::getUserId, userId)
                        .eq(KnowledgeBaseFavorite::getKnowledgeBaseId, kbId));
    }

    public boolean isSubscribed(Long userId, Long kbId) {
        if (userId == null || kbId == null) return false;
        return knowledgeBaseFavoriteMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseFavorite>()
                        .eq(KnowledgeBaseFavorite::getUserId, userId)
                        .eq(KnowledgeBaseFavorite::getKnowledgeBaseId, kbId)) > 0;
    }

    private KnowledgeBaseVO toVO(KnowledgeBase kb) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        vo.setId(kb.getId());
        vo.setName(kb.getName());
        vo.setCover(kb.getCover());
        vo.setDescription(kb.getDescription());
        vo.setVisibility(kb.getVisibility() != null ? kb.getVisibility() : VISIBILITY_PRIVATE);
        vo.setOwnerId(kb.getUserId());
        vo.setOwnerName(null);
        vo.setOwnerAvatar(null);
        vo.setSubCount(countSubByKbId(kb.getId()));
        vo.setContentCount(countContentByKbId(kb.getId()));
        vo.setCreatedAt(kb.getCreatedAt() != null ? kb.getCreatedAt().format(DATE_FORMAT) : null);
        vo.setUpdatedAt(kb.getUpdatedAt() != null ? kb.getUpdatedAt().format(DATE_FORMAT) : null);
        return vo;
    }

    private int countSubByKbId(Long kbId) {
        if (kbId == null) return 0;
        Long n = knowledgeBaseFavoriteMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseFavorite>().eq(KnowledgeBaseFavorite::getKnowledgeBaseId, kbId));
        return n != null ? n.intValue() : 0;
    }

    private int countContentByKbId(Long kbId) {
        if (kbId == null) return 0;
        Long n = knowledgeBaseContentMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseContent>().eq(KnowledgeBaseContent::getKnowledgeBaseId, kbId));
        return n != null ? n.intValue() : 0;
    }

    private KnowledgeBaseContentItemVO toContentItemVO(Content c) {
        KnowledgeBaseContentItemVO vo = new KnowledgeBaseContentItemVO();
        vo.setId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setSummary(c.getSummary());
        vo.setCover(c.getCover());
        return vo;
    }
}
