package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.dto.BlogBotVO;
import com.blog.content.dto.CreateBlogBotRequest;
import com.blog.content.entity.BlogBot;
import com.blog.content.entity.Tag;
import com.blog.content.mapper.BlogBotMapper;
import com.blog.content.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogBotService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BlogBotMapper blogBotMapper;
    private final TagMapper tagMapper;

    public List<BlogBotVO> listMyBots(Long userId) {
        LambdaQueryWrapper<BlogBot> q = new LambdaQueryWrapper<>();
        q.eq(BlogBot::getUserId, userId)
                .orderByDesc(BlogBot::getUpdatedAt)
                .orderByDesc(BlogBot::getCreatedAt);
        List<BlogBot> list = blogBotMapper.selectList(q);
        return list.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public BlogBotVO createBot(Long userId, CreateBlogBotRequest request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "机器人名称不能为空");
        }
        if (name.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "机器人名称最长 32 字符");
        }
        String style = request.getStyle() != null ? request.getStyle().trim() : "professional";
        String summaryStyle = request.getSummaryStyle() != null && !request.getSummaryStyle().trim().isEmpty()
                ? request.getSummaryStyle().trim() : "concise";
        String wordCountPreference = request.getWordCountPreference() != null && !request.getWordCountPreference().trim().isEmpty()
                ? request.getWordCountPreference().trim() : "medium";

        BlogBot bot = new BlogBot();
        bot.setUserId(userId);
        bot.setName(name);
        bot.setAvatar(request.getAvatar() != null && !request.getAvatar().trim().isEmpty() ? request.getAvatar().trim() : null);
        bot.setStyle(style);
        bot.setMainTagId(request.getMainTagId());
        bot.setSummaryStyle(summaryStyle);
        bot.setWordCountPreference(wordCountPreference);
        blogBotMapper.insert(bot);
        BlogBot saved = blogBotMapper.selectById(bot.getId());
        return toVO(saved != null ? saved : bot);
    }

    private BlogBotVO toVO(BlogBot b) {
        BlogBotVO vo = new BlogBotVO();
        vo.setId(b.getId());
        vo.setName(b.getName());
        vo.setAvatar(b.getAvatar());
        vo.setStyle(b.getStyle());
        vo.setMainTagId(b.getMainTagId());
        vo.setMainTagName(resolveMainTagName(b.getMainTagId()));
        vo.setSummaryStyle(b.getSummaryStyle());
        vo.setWordCountPreference(b.getWordCountPreference());
        vo.setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().format(DATE_FORMAT) : null);
        vo.setUpdatedAt(b.getUpdatedAt() != null ? b.getUpdatedAt().format(DATE_FORMAT) : null);
        return vo;
    }

    private String resolveMainTagName(Long mainTagId) {
        if (mainTagId == null) return null;
        Tag tag = tagMapper.selectById(mainTagId);
        return tag != null ? tag.getName() : null;
    }
}
