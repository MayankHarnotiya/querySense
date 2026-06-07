package com.querySense.schema;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaService {

    private final JdbcTemplate analyticsJdbcTemplate;
    private volatile String cachedSchema;

    public SchemaService(@Qualifier("analyticsJdbcTemplate") JdbcTemplate analyticsJdbcTemplate) {
        this.analyticsJdbcTemplate = analyticsJdbcTemplate;
    }

    public String getSchemaDescription() {
        if (cachedSchema == null) {
            cachedSchema = loadSchema();
        }
        return cachedSchema;
    }

    public java.util.Set<String> getTableNames() {
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                """;
        return new java.util.HashSet<>(
                analyticsJdbcTemplate.queryForList(sql, String.class)
        );
    }
    /** Clears the cached schema so it is rebuilt (e.g. after a new table is uploaded). */
    public void refresh() {
        cachedSchema = null;
    }
    private String loadSchema() {
        String sql = """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                ORDER BY table_name, ordinal_position
                """;

        List<Map<String, Object>> rows = analyticsJdbcTemplate.queryForList(sql);

        // Group columns under each table, preserving order
        Map<String, List<String>> tables = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String table = (String) row.get("table_name");
            String column = (String) row.get("column_name");
            tables.computeIfAbsent(table, t -> new ArrayList<>()).add(column);
        }

        // Build a compact description like:  customers(id, name, city, created_at)
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : tables.entrySet()) {
            sb.append(entry.getKey())
                    .append("(")
                    .append(String.join(", ", entry.getValue()))
                    .append(")\n");
        }
        return sb.toString().trim();
    }
}