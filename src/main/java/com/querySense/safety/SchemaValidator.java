package com.querySense.safety;

import com.querySense.schema.SchemaService;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class SchemaValidator {

    private final SchemaService schemaService;

    public SchemaValidator(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    public void validate(String sql) {
        Set<String> realTables = schemaService.getTableNames();

        List<String> usedTables;
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            usedTables = new TablesNamesFinder().getTableList(statement);
        } catch (Exception e) {
            throw new UnsafeSqlException("Could not analyze the generated SQL.");
        }

        for (String table : usedTables) {
            String clean = table.replace("\"", "").toLowerCase();
            if (!realTables.contains(clean)) {
                throw new UnsafeSqlException(
                        "Query references a table that does not exist: " + clean);
            }
        }
    }
}