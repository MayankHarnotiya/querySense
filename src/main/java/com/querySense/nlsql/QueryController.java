package com.querySense.nlsql;

import com.querySense.cache.QueryCacheService;
import com.querySense.cache.RateLimitService;
import com.querySense.execution.SqlExecutionService;
import com.querySense.safety.SchemaValidator;
import com.querySense.safety.SqlSafetyValidator;
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

    public QueryController(NlToSqlService nlToSqlService,
                           SqlSafetyValidator sqlSafetyValidator,
                           SchemaValidator schemaValidator,
                           SqlExecutionService sqlExecutionService,
                           QueryCacheService queryCacheService,
                           RateLimitService rateLimitService) {
        this.nlToSqlService = nlToSqlService;
        this.sqlSafetyValidator = sqlSafetyValidator;
        this.schemaValidator = schemaValidator;
        this.sqlExecutionService = sqlExecutionService;
        this.queryCacheService = queryCacheService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(
            @Valid @RequestBody QueryRequest request,
            HttpServletRequest httpRequest,
            Principal principal) {

        // 0) Rate limit — by caller IP for now
        String clientId = (principal != null)
                ? principal.getName()                 // the authenticated username
                : httpRequest.getRemoteAddr();        // fallback (shouldn't happen on a secured endpoint)
        if (!rateLimitService.allow(clientId)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)   // 429
                    .body(Map.of("error", "Rate limit exceeded. Try again shortly."));
        }

        String question = request.question();

        // 1) Cache hit? Return immediately — no AI, no DB.
        Optional<List<Map<String, Object>>> cached = queryCacheService.get(question);
        if (cached.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "question", question,
                    "rowCount", cached.get().size(),
                    "rows", cached.get(),
                    "cached", true
            ));
        }

        // 2) Miss → full pipeline
        String sql = nlToSqlService.generateSql(question);
        sqlSafetyValidator.validate(sql);
        schemaValidator.validate(sql);
        List<Map<String, Object>> rows = sqlExecutionService.run(sql);

        // 3) Store for next time
        queryCacheService.put(question, rows);

        return ResponseEntity.ok(Map.of(
                "question", question,
                "generatedSql", sql,
                "rowCount", rows.size(),
                "rows", rows,
                "cached", false
        ));
    }
}