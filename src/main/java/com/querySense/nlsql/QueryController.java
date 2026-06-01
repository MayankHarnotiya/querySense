package com.querySense.nlsql;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final NlToSqlService nlToSqlService;

    public QueryController(NlToSqlService nlToSqlService) {
        this.nlToSqlService = nlToSqlService;
    }

    @PostMapping("/query")
    public Map<String, String> query(@Valid @RequestBody QueryRequest request) {
        String sql = nlToSqlService.generateSql(request.question());
        return Map.of(
                "question", request.question(),
                "generatedSql", sql
        );
    }
}