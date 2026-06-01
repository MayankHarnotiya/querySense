package com.querySense.safety;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

@Component
public class SqlSafetyValidator {

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new UnsafeSqlException("Generated SQL was empty.");
        }

        // 1) Parse. If it isn't valid SQL at all, reject.
        Statements parsed;
        try {
            parsed = CCJSqlParserUtil.parseStatements(sql);
        } catch (JSQLParserException e) {
            throw new UnsafeSqlException("Could not parse the generated SQL.");
        }

        // 2) Must be exactly ONE statement (blocks stacked queries).
        if (parsed.getStatements().size() != 1) {
            throw new UnsafeSqlException(
                    "Only a single SQL statement is allowed.");
        }

        // 3) That one statement must be a SELECT (blocks INSERT/UPDATE/DELETE/DROP/etc).
        Statement statement = parsed.getStatements().get(0);
        if (!(statement instanceof Select)) {
            throw new UnsafeSqlException(
                    "Only read-only SELECT queries are allowed.");
        }
    }
}