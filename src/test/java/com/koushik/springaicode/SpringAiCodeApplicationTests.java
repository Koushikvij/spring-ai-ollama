package com.koushik.springaicode;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class SpringAiCodeApplicationTests {

    @MockBean
    private OllamaChatModel ollamaChatModel;

    @MockBean
    private EmbeddingModel embeddingModel;

    @Test
    void contextLoads() {
    }

}
