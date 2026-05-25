package com.koushik.springaicode;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Smoke test — verifies the Spring context starts without errors.
 *
 * External dependencies (Ollama, PostgreSQL) are replaced with Mockito
 * mock beans so the test runs without any infrastructure.
 *
 * JdbcTemplate is mocked and stubbed to return a positive row count so
 * DataInitializer skips the product insertion on startup.
 */
@SpringBootTest
class SpringAiCodeApplicationTests {

    @MockBean
    private OllamaChatModel ollamaChatModel;

    @MockBean
    private EmbeddingModel embeddingModel;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        // Stub count so DataInitializer treats the store as already populated
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
    }
}
