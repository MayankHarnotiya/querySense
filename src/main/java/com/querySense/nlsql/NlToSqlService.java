package com.querySense.nlsql;

import com.querySense.schema.SchemaService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class NlToSqlService {

    private final ChatClient chatClient;
    private final SchemaService schemaService;

    public NlToSqlService(ChatClient.Builder chatClientBuilder,
                          SchemaService schemaService) {
        this.chatClient = chatClientBuilder.build();
        this.schemaService = schemaService;
    }

    public String generateSql(String question) {
        String schema = schemaService.getSchemaDescription();

        String systemPrompt = """
                You are a SQL generator for a PostgreSQL database.
                Convert the user's question into a single read-only SELECT query.

                The database has EXACTLY these tables and columns:
                %s

                Rules:
                - Use ONLY the tables and columns listed above. Do not invent names.
                - Output ONLY the raw SQL. No explanations, no markdown, no backticks.
                - Use a single SELECT statement only.
                """.formatted(schema);

        String raw = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        return clean(raw);
    }

    private String clean(String sql) {
        if (sql == null) return "";
        String cleaned = sql.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("(?s)```(?:sql)?", "").trim();
        }
        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }
}