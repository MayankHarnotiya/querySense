package com.querySense.ingest;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class AnalyticsAdminDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.analytics-admin")
    public DataSource analyticsAdminDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    public JdbcTemplate analyticsAdminJdbcTemplate(
            @Qualifier("analyticsAdminDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}