package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.dto.CollectionFolderVO;
import com.blog.content.dto.ContentListItemVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.dto.CreateFolderRequest;
import com.blog.content.dto.UpdateFolderRequest;
import com.blog.content.entity.CollectionFolder;
import com.blog.content.entity.ContentCollection;
import com.blog.content.mapper.CollectionFolderMapper;
import com.blog.content.mapper.ContentCollectionMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionFolderService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final CollectionFolderMapper collectionFolderMapper;
    private final ContentCollectionMapper contentCollectionMapper;
    private final ContentService contentService;

    public List<CollectionFolderVO> listMyFolders(Long userId) {
        LambdaQueryWrapper<CollectionFolder> q = new LambdaQueryWrapper<>();
        q.eq(CollectionFolder::getUserId, userId)
                .orderByDesc(CollectionFolder::getIsDefault)  // 默认收藏夹排最前
                .orderByDesc(CollectionFolder::getCreatedAt);
        List<CollectionFolder> folders = collectionFolderMapper.selectList(q);
        return folders.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /** 获取单个收藏夹详情（需为当前用户） */
    public CollectionFolderVO getFolder(Long userId, Long folderId) {
        CollectionFolder folder = collectionFolderMapper.selectById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏夹不存在");
        }
        return toVO(folder);
    }

    /** 分页获取收藏夹内的文章列表（仅已发布，按收藏时间倒序） */
    public ContentsMeResponse listContentsInFolder(Long userId, Long folderId, int page, int pageSize) {
        CollectionFolder folder = collectionFolderMapper.selectById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏夹不存在");
        }
        long total = contentCollectionMapper.selectCount(
                new LambdaQueryWrapper<ContentCollection>().eq(ContentCollection::getFolderId, folderId));
        if (total == 0) {
            ContentsMeResponse empty = new ContentsMeResponse();
            empty.setList(List.of());
            empty.setTotal(0L);
            return empty;
        }
        IPage<ContentCollection> p = contentCollectionMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<ContentCollection>()
                        .eq(ContentCollection::getFolderId, folderId)
                        .orderByDesc(ContentCollection::getCreatedAt));
        List<Long> contentIds = p.getRecords().stream()
                .map(ContentCollection::getContentId)
                .collect(Collectors.toList());
        List<ContentListItemVO> list = contentService.listByIds(contentIds).stream()
                .filter(vo -> STATUS_PUBLISHED.equals(vo.getStatus()))
                .collect(Collectors.toList());
        ContentsMeResponse res = new ContentsMeResponse();
        res.setList(list);
        res.setTotal(total);
        return res;
    }

    public CollectionFolderVO createFolder(Long userId, CreateFolderRequest request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收藏夹名称不能为空");
        }
        CollectionFolder folder = new CollectionFolder();
        folder.setUserId(userId);
        folder.setName(name);
        folder.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        folder.setIsDefault(false);
        collectionFolderMapper.insert(folder);
        // 重新查询以获取数据库回填的 createdAt（insert 后实体上可能未回填）
        CollectionFolder saved = collectionFolderMapper.selectById(folder.getId());
        if (saved != null && saved.getCreatedAt() == null) {
            saved.setCreatedAt(LocalDateTime.now());
        }
        return toVO(saved != null ? saved : folder);
    }

    public CollectionFolderVO updateFolder(Long userId, Long folderId, UpdateFolderRequest request) {
        CollectionFolder folder = collectionFolderMapper.selectById(folderId);
        if (folder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏夹不存在");
        }
        if (Boolean.TRUE.equals(folder.getIsDefault())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "默认收藏夹不能修改名字和简介");
        }
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收藏夹名称不能为空");
            }
            folder.setName(name);
        }
        if (request.getDescription() != null) {
            folder.setDescription(request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
        }
        collectionFolderMapper.updateById(folder);
        return toVO(folder);
    }

    public void deleteFolder(Long userId, Long folderId) {
        CollectionFolder folder = collectionFolderMapper.selectById(folderId);
        if (folder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏夹不存在");
        }
        if (Boolean.TRUE.equals(folder.getIsDefault())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能删除默认收藏夹");
        }
        collectionFolderMapper.deleteById(folderId);
    }

    /** 将文章加入收藏夹（仅已发布文章；重复加入同一收藏夹会报错） */
    public void addContentToFolder(Long userId, Long folderId, Long contentId) {
        CollectionFolder folder = collectionFolderMapper.selectById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏夹不存在");
        }
        List<ContentListItemVO> list = contentService.listByIds(List.of(contentId));
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        if (!STATUS_PUBLISHED.equals(list.get(0).getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可收藏已发布的文章");
        }
        long exists = contentCollectionMapper.selectCount(
                new LambdaQueryWrapper<ContentCollection>()
                        .eq(ContentCollection::getFolderId, folderId)
                        .eq(ContentCollection::getContentId, contentId));
        if (exists > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该文章已在收藏夹中");
        }
        ContentCollection cc = new ContentCollection();
        cc.setUserId(userId);
        cc.setFolderId(folderId);
        cc.setContentId(contentId);
        cc.setCreatedAt(LocalDateTime.now());
        contentCollectionMapper.insert(cc);
        contentService.incrementCollectionCount(contentId);
    }

    /** 从收藏夹移除文章 */
    public void removeContentFromFolder(Long userId, Long folderId, Long contentId) {
        CollectionFolder folder = collectionFolderMapper.selectById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏夹不存在");
        }
        long deleted = contentCollectionMapper.delete(
                new LambdaQueryWrapper<ContentCollection>()
                        .eq(ContentCollection::getFolderId, folderId)
                        .eq(ContentCollection::getContentId, contentId)
                        .eq(ContentCollection::getUserId, userId));
        if (deleted > 0) {
            contentService.decrementCollectionCount(contentId);
        }
    }

    /** 当前用户收藏夹中包含该文章的收藏夹 ID 列表（用于文章页展示「已添加」） */
    public List<Long> getFolderIdsContainingContent(Long userId, Long contentId) {
        List<ContentCollection> list = contentCollectionMapper.selectList(
                new LambdaQueryWrapper<ContentCollection>()
                        .eq(ContentCollection::getUserId, userId)
                        .eq(ContentCollection::getContentId, contentId));
        return list.stream().map(ContentCollection::getFolderId).distinct().collect(Collectors.toList());
    }

    private CollectionFolderVO toVO(CollectionFolder f) {
        CollectionFolderVO vo = new CollectionFolderVO();
        vo.setId(f.getId());
        vo.setName(f.getName());
        vo.setDescription(f.getDescription());
        vo.setIsDefault(Boolean.TRUE.equals(f.getIsDefault()));
        vo.setCount(countByFolderId(f.getId()));
        vo.setCreatedAt(f.getCreatedAt() != null ? f.getCreatedAt().format(DATE_FORMAT) : null);
        return vo;
    }

    private int countByFolderId(Long folderId) {
        Long n = contentCollectionMapper.selectCount(
                new LambdaQueryWrapper<ContentCollection>().eq(ContentCollection::getFolderId, folderId));
        return n != null ? n.intValue() : 0;
    }
}
