package com.blog.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.user.dto.UserMessagePageResponse;
import com.blog.user.dto.UserMessageVO;
import com.blog.user.entity.UserMessage;
import com.blog.user.mapper.UserMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMessageService {

    private final UserMessageMapper userMessageMapper;

    public UserMessagePageResponse list(Long userId, int page, int pageSize, boolean unreadOnly) {
        Page<UserMessage> pg = new Page<>(Math.max(1, page), pageSize <= 0 ? 20 : Math.min(100, pageSize));
        LambdaQueryWrapper<UserMessage> q = new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId);
        if (unreadOnly) q.isNull(UserMessage::getReadAt);
        q.orderByDesc(UserMessage::getCreatedAt).orderByDesc(UserMessage::getId);
        Page<UserMessage> result = userMessageMapper.selectPage(pg, q);
        UserMessagePageResponse res = new UserMessagePageResponse();
        res.setTotal(result.getTotal());
        res.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return res;
    }

    public long unreadCount(Long userId) {
        return userMessageMapper.selectCount(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .isNull(UserMessage::getReadAt));
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        UserMessage m = userMessageMapper.selectById(id);
        if (m == null || !userId.equals(m.getUserId())) return;
        if (m.getReadAt() == null) {
            m.setReadAt(LocalDateTime.now());
            userMessageMapper.updateById(m);
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        var list = userMessageMapper.selectList(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .isNull(UserMessage::getReadAt));
        if (list.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (UserMessage m : list) {
            m.setReadAt(now);
            userMessageMapper.updateById(m);
        }
    }

    private UserMessageVO toVO(UserMessage m) {
        UserMessageVO vo = new UserMessageVO();
        vo.setId(m.getId());
        vo.setSenderUserId(m.getSenderUserId());
        vo.setTitle(m.getTitle());
        vo.setBody(m.getBody());
        vo.setMsgType(m.getMsgType());
        vo.setScene(m.getScene());
        vo.setExtra(m.getExtra());
        vo.setRead(m.getReadAt() != null);
        vo.setReadAt(m.getReadAt());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }
}
