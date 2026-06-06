package com.querySense.nlsql;

import com.querySense.audit.AuditService;
import com.querySense.cache.QueryCacheService;
import com.querySense.cache.RateLimitService;
import com.querySense.execution.SqlExecutionService;
import com.querySense.safety.SchemaValidator;
import com.querySense.safety.SqlSafetyValidator;
import com.querySense.safety.UnsafeSqlException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final NlToSqlService nlToSqlService;
    private final SqlSafetyValidator sqlSafetyValidator;
    private final SchemaValidator schemaValidator;
    private final SqlExecutionService sqlExecutionService;
    private final QueryCacheService queryCacheService;
    private final RateLimitService rateLimitService;
    private final AuditService auditService;

    public QueryController(NlToSqlService nlToSqlService,
                           SqlSafetyValidator sqlSafetyValidator,
                           SchemaValidator schemaValidator,
                           SqlExecutionService sqlExecutionService,
                           QueryCacheService queryCacheService,
                           RateLimitService rateLimitService,
                           AuditService auditService) {
        this.nlToSqlService = nlToSqlService;
        this.sqlSafetyValidator = sqlSafetyValidator;
        this.schemaValidator = schemaValidator;
        this.sqlExecutionService = sqlExecutionService;
        this.queryCacheService = queryCacheService;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(
            @Valid @RequestBody QueryRequest request,
            HttpServletRequest httpRequest,
            Principal principal) {

        // 0) Rate limit — keyed to the authenticated username
        String clientId = (principal != null)
                ? principal.getName()
                : httpRequest.getRemoteAddr();
        if (!rateLimitService.allow(clientId)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded. Try again shortly."));
        }

        String question = request.question();

        // Effective paging: defaults + a hard cap on size
        int page = (request.page() != null && request.page() >= 0) ? request.page() : 0;
        int size = (request.size() != null && request.size() > 0)
                ? Math.min(request.size(), 200)   // cap at 200 rows per page
                : 50;                             // default page size

        // 1) Cache hit? (per question + page + size)
        Optional<List<Map<String, Object>>> cached = queryCacheService.get(question, page, size);
        if (cached.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "question", question,
                    "page", page,
                    "size", size,
                    "rowCount", cached.get().size(),
                    "rows", cached.get(),
                    "cached", true
            ));
        }

        // 2) Miss → full pipeline (with auditing)
        String sql = null;
        try {
            sql = nlToSqlService.generateSql(question);
            sqlSafetyValidator.validate(sql);
            schemaValidator.validate(sql);
            List<Map<String, Object>> rows = sqlExecutionService.run(sql, page, size);

            queryCacheService.put(question, rows, page, size);
            auditService.record(clientId, question, sql, "SUCCESS", rows.size(), null);

            return ResponseEntity.ok(Map.of(
                    "question", question,
                    "generatedSql", sql,
                    "page", page,
                    "size", size,
                    "rowCount", rows.size(),
                    "rows", rows,
                    "cached", false
            ));
        } catch (UnsafeSqlException ex) {
            auditService.record(clientId, question, sql, "BLOCKED", null, ex.getMessage());
            throw ex;
        }
    }
}