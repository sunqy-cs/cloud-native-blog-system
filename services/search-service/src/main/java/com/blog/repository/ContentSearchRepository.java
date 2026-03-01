package com.blog.repository;

import com.blog.document.ContentDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentSearchRepository extends ElasticsearchRepository<ContentDocument, Long> {
}
