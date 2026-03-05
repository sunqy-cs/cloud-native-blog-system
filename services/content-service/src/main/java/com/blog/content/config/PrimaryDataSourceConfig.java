package com.blog.content.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 主数据源（MySQL）：知识库、内容等业务表。
 * 显式声明并标记 @Primary，避免与 vector 数据源冲突导致 MyBatis 误用 PostgreSQL。
 */
@Configuration
public class PrimaryDataSourceConfig {

    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}
