package com.querySense.nlsql;

import com.querySense.execution.SqlExecutionService;
import com.querySense.safety.SchemaValidator;
import com.querySense.safety.SqlSafetyValidator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final NlToSqlService nlToSqlService;
    private final SqlSafetyValidator sqlSafetyValidator;
    private final SchemaValidator schemaValidator;
    private final SqlExecutionService sqlExecutionService;

    public QueryController(NlToSqlService nlToSqlService,
                           SqlSafetyValidator sqlSafetyValidator,
                           SchemaValidator schemaValidator,
                           SqlExecutionService sqlExecutionService) {
        this.nlToSqlService = nlToSqlService;
        this.sqlSafetyValidator = sqlSafetyValidator;
        this.schemaValidator = schemaValidator;
        this.sqlExecutionService = sqlExecutionService;
    }

    @PostMapping("/query")
    public Map<String, Object> query(@Valid @RequestBody QueryRequest request) {
        String sql = nlToSqlService.generateSql(request.question());

        // SAFETY GATES — both run before any execution
        sqlSafetyValidator.validate(sql);   // single-statement, SELECT-only
        schemaValidator.validate(sql);      // tables must actually exist

        List<Map<String, Object>> rows = sqlExecutionService.run(sql);

        return Map.of(
                "question", request.question(),
                "generatedSql", sql,
                "rowCount", rows.size(),
                "rows", rows
        );
    }
}