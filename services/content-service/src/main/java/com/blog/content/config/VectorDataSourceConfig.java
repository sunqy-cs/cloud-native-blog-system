package com.blog.content.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PostgreSQL + pgvector 数据源，仅用于 RAG 向量表 kb_chunk_embedding 的读写。
 */
@Configuration
public class VectorDataSourceConfig {

    @Bean(name = "vectorDataSource")
    @ConfigurationProperties(prefix = "app.vector-datasource")
    public DataSource vectorDataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }
}
