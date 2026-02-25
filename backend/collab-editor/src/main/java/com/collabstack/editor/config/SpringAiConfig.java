package com.collabstack.editor.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SpringAiConfig {

    /**
     * ChatClient is always available (requires only OpenAI API key).
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * VectorStore only created when app.rag.enabled=true.
     * Requires pgvector extension installed in PostgreSQL.
     * Enable in Docker environment where pgvector/pgvector:pg16 image is used.
     */
    @Bean
    @ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
    public VectorStore vectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        try {
            // Try to enable pgvector extension if not already enabled
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        } catch (Exception e) {
            throw new IllegalStateException(
                "pgvector extension is not installed. Please install pgvector: " +
                "https://github.com/pgvector/pgvector#installation", e);
        }
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(768)
                .initializeSchema(true)
                .build();
    }
}
