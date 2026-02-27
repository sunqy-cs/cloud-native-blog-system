package com.blog.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.ai.dto.BotVO;
import com.blog.ai.dto.CreateBotRequest;
import com.blog.ai.entity.Bot;
import com.blog.ai.entity.Tag;
import com.blog.ai.mapper.BotMapper;
import com.blog.ai.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BotService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BotMapper botMapper;
    private final TagMapper tagMapper;

    public List<BotVO> listMyBots(Long userId) {
        LambdaQueryWrapper<Bot> q = new LambdaQueryWrapper<>();
        q.eq(Bot::getUserId, userId)
                .orderByDesc(Bot::getUpdatedAt)
                .orderByDesc(Bot::getCreatedAt);
        List<Bot> list = botMapper.selectList(q);
        return list.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public BotVO createBot(Long userId, CreateBotRequest request) {
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

        Bot bot = new Bot();
        bot.setUserId(userId);
        bot.setName(name);
        bot.setAvatar(request.getAvatar() != null && !request.getAvatar().trim().isEmpty() ? request.getAvatar().trim() : null);
        bot.setStyle(style);
        bot.setMainTagId(request.getMainTagId());
        bot.setSummaryStyle(summaryStyle);
        bot.setWordCountPreference(wordCountPreference);
        botMapper.insert(bot);
        Bot saved = botMapper.selectById(bot.getId());
        return toVO(saved != null ? saved : bot);
    }

    public void deleteBot(Long userId, Long botId) {
        if (botId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "机器人不存在");
        }
        Bot bot = botMapper.selectById(botId);
        if (bot == null || !userId.equals(bot.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "机器人不存在或无权删除");
        }
        botMapper.deleteById(botId);
    }

    private BotVO toVO(Bot b) {
        BotVO vo = new BotVO();
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
