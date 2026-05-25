package com.koushik.springaicode.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AppConfig {

    // @Bean
    // public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    //     return SimpleVectorStore.builder(embeddingModel).build();
    // }

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore
            .builder(jdbcTemplate, embeddingModel)
            .dimensions(1024)
            .build();
    }

}
