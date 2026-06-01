package com.querySense.execution;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SqlExecutionService {

    private final JdbcTemplate analyticsJdbcTemplate;

    public SqlExecutionService(
            @Qualifier("analyticsJdbcTemplate") JdbcTemplate analyticsJdbcTemplate) {
        this.analyticsJdbcTemplate = analyticsJdbcTemplate;
    }

    public List<Map<String, Object>> run(String sql) {
        return analyticsJdbcTemplate.queryForList(sql);
    }
}