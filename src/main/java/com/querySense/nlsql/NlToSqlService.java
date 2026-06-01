package com.querySense.nlsql;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class NlToSqlService {

    private final ChatClient chatClient;

    public NlToSqlService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateSql(String question) {
        String systemPrompt = """
                You are a SQL generator. Convert the user's question into a single
                read-only PostgreSQL SELECT query.
                Rules:
                - Output ONLY the raw SQL. No explanations, no markdown, no backticks.
                - Use only a standard SELECT statement.
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }
}