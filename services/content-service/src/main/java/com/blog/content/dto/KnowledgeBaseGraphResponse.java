package com.blog.content.dto;

import lombok.Data;

import java.util.List;

/** 知识图谱：节点与边，用于前端力导向图 */
@Data
public class KnowledgeBaseGraphResponse {
    private List<GraphNodeVO> nodes;
    private List<GraphLinkVO> links;

    @Data
    public static class GraphNodeVO {
        private Long id;
        private String title;
        /** BLOG / KNOWLEDGE */
        private String type;
    }

    @Data
    public static class GraphLinkVO {
        private Long source;
        private Long target;
    }
}
