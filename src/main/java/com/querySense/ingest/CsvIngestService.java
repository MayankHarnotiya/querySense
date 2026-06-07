package com.querySense.ingest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CsvIngestService {

    private final JdbcTemplate admin;

    public CsvIngestService(@Qualifier("analyticsAdminJdbcTemplate") JdbcTemplate admin) {
        this.admin = admin;
    }

    /** Parse a CSV, (re)create a table from it, insert the rows. Returns a small summary. */
    public Map<String, Object> ingest(MultipartFile file, String requestedTable) throws Exception {
        String table = sanitize(
                (requestedTable != null && !requestedTable.isBlank())
                        ? requestedTable
                        : stripExtension(file.getOriginalFilename()));
        if (table.isBlank()) table = "uploaded_data";

        List<String> headers;
        List<CSVRecord> records;
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setTrim(true).build().parse(reader)) {
            headers = parser.getHeaderNames();
            records = parser.getRecords();
        }
        if (headers == null || headers.isEmpty())
            throw new IllegalArgumentException("CSV has no header row.");

        // sanitize + de-duplicate column names
        List<String> cols = new ArrayList<>();
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (String h : headers) {
            String c = sanitize(h.isBlank() ? "col" : h);
            if (c.isBlank()) c = "col";
            int n = seen.merge(c, 1, Integer::sum);
            cols.add(n == 1 ? c : c + "_" + n);
        }

        // infer a simple type per column: BIGINT, NUMERIC, or TEXT
        String[] types = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) types[i] = inferType(records, i);

        // (re)create the table
        StringBuilder create = new StringBuilder("CREATE TABLE \"" + table + "\" (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) create.append(", ");
            create.append("\"").append(cols.get(i)).append("\" ").append(types[i]);
        }
        create.append(")");
        admin.execute("DROP TABLE IF EXISTS \"" + table + "\"");
        admin.execute(create.toString());

        // let the read-only query user read it
        admin.execute("GRANT SELECT ON \"" + table + "\" TO analytics_readonly");

        // bulk insert
        String placeholders = String.join(", ", java.util.Collections.nCopies(cols.size(), "?"));
        String colList = String.join(", ", cols.stream().map(c -> "\"" + c + "\"").toList());
        String insert = "INSERT INTO \"" + table + "\" (" + colList + ") VALUES (" + placeholders + ")";

        admin.batchUpdate(insert, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int rowIdx) throws SQLException {
                CSVRecord rec = records.get(rowIdx);
                for (int i = 0; i < cols.size(); i++) {
                    String raw = i < rec.size() ? rec.get(i) : null;
                    setTyped(ps, i + 1, raw, types[i]);
                }
            }
            public int getBatchSize() { return records.size(); }
        });

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("table", table);
        summary.put("columns", cols);
        summary.put("rowsInserted", records.size());
        return summary;
    }

    private void setTyped(PreparedStatement ps, int idx, String raw, String type) throws SQLException {
        if (raw == null || raw.isBlank()) { ps.setNull(idx, Types.NULL); return; }
        try {
            switch (type) {
                case "BIGINT" -> ps.setLong(idx, Long.parseLong(raw.trim()));
                case "NUMERIC" -> ps.setBigDecimal(idx, new BigDecimal(raw.trim()));
                default -> ps.setString(idx, raw);
            }
        } catch (NumberFormatException e) {
            ps.setString(idx, raw); // fall back to text if a value doesn't parse
        }
    }

    private String inferType(List<CSVRecord> records, int col) {
        boolean allInt = true, allNum = true, any = false;
        for (CSVRecord r : records) {
            if (col >= r.size()) continue;
            String v = r.get(col);
            if (v == null || v.isBlank()) continue;
            any = true;
            v = v.trim();
            if (allInt) { try { Long.parseLong(v); } catch (Exception e) { allInt = false; } }
            if (allNum) { try { new BigDecimal(v); } catch (Exception e) { allNum = false; } }
        }
        if (!any) return "TEXT";
        if (allInt) return "BIGINT";
        if (allNum) return "NUMERIC";
        return "TEXT";
    }

    private String sanitize(String s) {
        if (s == null) return "";
        String x = s.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        if (!x.isEmpty() && Character.isDigit(x.charAt(0))) x = "t_" + x;
        return x;
    }

    private String stripExtension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}