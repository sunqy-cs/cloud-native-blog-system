package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.dto.CollectionFolderVO;
import com.blog.content.dto.CreateFolderRequest;
import com.blog.content.dto.UpdateFolderRequest;
import com.blog.content.entity.CollectionFolder;
import com.blog.content.entity.ContentCollection;
import com.blog.content.mapper.CollectionFolderMapper;
import com.blog.content.mapper.ContentCollectionMapper;
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

    private final CollectionFolderMapper collectionFolderMapper;
    private final ContentCollectionMapper contentCollectionMapper;

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
