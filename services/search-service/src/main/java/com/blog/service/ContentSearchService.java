package com.blog.service;

import com.blog.document.ContentDocument;
import com.blog.dto.IndexRequestDto;
import com.blog.repository.ContentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentSearchService {

    private final ContentSearchRepository contentSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /** 索引单篇文章（content-service 发布时调用） */
    public void index(IndexRequestDto dto) {
        if (dto == null || dto.getId() == null) return;
        try {
            ContentDocument doc = new ContentDocument();
            doc.setId(dto.getId());
            doc.setUserId(dto.getUserId());
            doc.setTitle(dto.getTitle() != null ? dto.getTitle() : "");
            doc.setSummary(dto.getSummary() != null ? dto.getSummary() : "");
            doc.setBody(dto.getBody() != null ? dto.getBody() : "");
            doc.setTagNames(dto.getTagNames() != null ? dto.getTagNames() : List.of());
            String publishedAtStr = formatPublishedAt(dto.getPublishedAt());
            doc.setPublishedAt(publishedAtStr);
            contentSearchRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to index content {}: {}", dto.getId(), e.getMessage());
        }
    }

    /** 从索引中删除 */
    public void delete(Long contentId) {
        if (contentId == null) return;
        try {
            contentSearchRepository.deleteById(contentId);
        } catch (Exception e) {
            log.warn("Failed to delete content from index {}: {}", contentId, e.getMessage());
        }
    }

    private static final DateTimeFormatter PUBLISHED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 综合搜索：标题、摘要、正文、标签。支持时间筛选与排序。
     * @param time all / 1d / 1w / 1m / 3m / 6m / 1y
     * @param sort comprehensive（相关度）/ newest（最新发布）/ likes（最多赞同，暂按相关度）
     */
    public List<Long> search(String keyword, int page, int pageSize, String sort, String time) {
        if (keyword == null || keyword.isBlank()) return List.of();
        String q = keyword.trim();
        log.debug("[Search] q={}, page={}, pageSize={}, sort={}, time={}", q, page, pageSize, sort, time);
        try {
            String queryJson = buildSearchQuery(q, time);
            log.debug("[Search] queryJson={}", queryJson);
            Query query = new StringQuery(queryJson)
                    .setPageable(PageRequest.of(page - 1, pageSize));
            if ("newest".equalsIgnoreCase(sort != null ? sort.trim() : "")) {
                query.addSort(Sort.by(Sort.Direction.DESC, "publishedAt"));
            }
            var searchHits = elasticsearchOperations.search(query, ContentDocument.class);
            List<Long> ids = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(ContentDocument::getId)
                    .collect(Collectors.toList());
            log.debug("[Search] totalHits={}, returnedIds={}", searchHits.getTotalHits(), ids);
            if (ids.isEmpty() && searchHits.getTotalHits() == 0) {
                log.debug("Search '{}' returned 0 hits (index may be empty, run reindex if needed)", q);
            }
            return ids;
        } catch (Exception e) {
            log.warn("Search failed for '{}': {}", q, e.getMessage());
            return List.of();
        }
    }

    /** 构建 ES 查询 JSON（multi_match 对中文更友好），支持可选时间过滤 */
    private String buildSearchQuery(String keyword, String time) {
        String escaped = keyword.replace("\\", "\\\\").replace("\"", "\\\"");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"bool\":{\"must\":[{\"multi_match\":{\"query\":\"");
        sb.append(escaped);
        sb.append("\",\"fields\":[\"title\",\"summary\",\"body\",\"tagNames\"],\"type\":\"best_fields\",\"operator\":\"or\"}}]");
        if (time != null && !time.isBlank() && !"all".equalsIgnoreCase(time.trim())) {
            LocalDateTime since = timeRangeStart(time.trim().toLowerCase());
            if (since != null) {
                String sinceStr = since.format(PUBLISHED_AT_FORMAT);
                log.debug("[Search] time filter: time={}, since={}, gte={}", time.trim(), since, sinceStr);
                sb.append(",\"filter\":[{\"range\":{\"publishedAt\":{\"gte\":\"");
                sb.append(sinceStr);
                sb.append("\"}}}]");
            } else {
                log.debug("[Search] time filter: time={} -> timeRangeStart null", time.trim());
            }
        } else {
            log.debug("[Search] no time filter (time={})", time);
        }
        sb.append("}}");
        return sb.toString();
    }

    /** 格式化为 yyyy-MM-dd'T'HH:mm:ss 写入 ES（Keyword 字段，字符串比较即可） */
    private String formatPublishedAt(String value) {
        if (value != null && !value.isBlank()) {
            String s = value.trim();
            if (s.length() >= 19) return s.substring(0, 19);
            if (s.length() == 10) return s + "T00:00:00";
            try {
                return LocalDateTime.parse(s, PUBLISHED_AT_FORMAT).format(PUBLISHED_AT_FORMAT);
            } catch (Exception ignored) { }
        }
        return LocalDateTime.now().format(PUBLISHED_AT_FORMAT);
    }

    /** 以东八区「当前时间」为基准计算起始时间，保证「一天内」等与用户预期一致 */
    private LocalDateTime timeRangeStart(String time) {
        LocalDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
        return switch (time) {
            case "1d" -> now.minusDays(1);
            case "1w" -> now.minusWeeks(1);
            case "1m" -> now.minusMonths(1);
            case "3m" -> now.minusMonths(3);
            case "6m" -> now.minusMonths(6);
            case "1y" -> now.minusYears(1);
            default -> null;
        };
    }
}
