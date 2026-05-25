package com.koushik.springaicode.controller;

import com.koushik.springaicode.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link OllamaController}.
 *
 * Starts a full Spring context with all external dependencies replaced by
 * Mockito mock beans (Ollama, PostgreSQL, EmbeddingService).
 * Uses MockMvc to drive HTTP requests without a real server.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OllamaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // --- mocked infrastructure ---
    @MockBean
    private OllamaChatModel ollamaChatModel;

    @MockBean
    private EmbeddingModel embeddingModel;      // needed by AppConfig / PgVectorStore

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private EmbeddingService embeddingService;

    // -------------------------------------------------------------------------
    // Shared setup
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        // Stub JdbcTemplate so DataInitializer skips product insertion on startup
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

        // Stub OllamaChatModel to return a fixed response for any prompt
        when(ollamaChatModel.call(any(Prompt.class))).thenReturn(buildMockChatResponse("Mocked LLM response"));
    }

    // -------------------------------------------------------------------------
    // GET /api/{message}  — basic chat
    // -------------------------------------------------------------------------

    @Test
    void chatEndpointReturns200WithLlmResponse() throws Exception {
        mockMvc.perform(get("/api/hello world"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mocked LLM response"));
    }

    // -------------------------------------------------------------------------
    // POST /api/recommend
    // -------------------------------------------------------------------------

    @Test
    void recommendEndpointReturns200WithLlmResponse() throws Exception {
        mockMvc.perform(post("/api/recommend")
                        .param("year", "2023")
                        .param("genre", "action")
                        .param("language", "English"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mocked LLM response"));
    }

    @Test
    void recommendEndpointReturnsBadRequestWhenParamsMissing() throws Exception {
        mockMvc.perform(post("/api/recommend")
                        .param("year", "2023"))
                // genre and language are missing — Spring should return 400
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/embedding
    // -------------------------------------------------------------------------

    @Test
    void embeddingEndpointReturnsFloatArray() throws Exception {
        when(embeddingService.embed("laptop stand")).thenReturn(new float[]{0.1f, 0.5f, 0.9f});

        mockMvc.perform(post("/api/embedding")
                        .param("text", "laptop stand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // -------------------------------------------------------------------------
    // POST /api/similarity
    // -------------------------------------------------------------------------

    @Test
    void similarityOfIdenticalVectorsIsOne() throws Exception {
        when(embeddingService.embed("cat")).thenReturn(new float[]{1.0f, 0.0f});
        when(embeddingService.embed("kitten")).thenReturn(new float[]{1.0f, 0.0f});

        mockMvc.perform(post("/api/similarity")
                        .param("text1", "cat")
                        .param("text2", "kitten"))
                .andExpect(status().isOk())
                .andExpect(content().string("1.0"));
    }

    @Test
    void similarityOfOrthogonalVectorsIsZero() throws Exception {
        when(embeddingService.embed("cat")).thenReturn(new float[]{1.0f, 0.0f});
        when(embeddingService.embed("database")).thenReturn(new float[]{0.0f, 1.0f});

        mockMvc.perform(post("/api/similarity")
                        .param("text1", "cat")
                        .param("text2", "database"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));
    }

    // -------------------------------------------------------------------------
    // POST /api/product  — vector store search
    // -------------------------------------------------------------------------

    @Test
    void productEndpointReturnsMatchingDocuments() throws Exception {
        Document doc = new Document(
                "Title: Bluetooth Speaker\nPrice: $39.99\nCategory: Electronics",
                Map.of("title", "Bluetooth Speaker", "category", "Electronics")
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        mockMvc.perform(post("/api/product")
                        .param("text", "bluetooth speaker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void productEndpointReturnsEmptyArrayWhenNothingMatches() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/product")
                        .param("text", "xyzzy unknown product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // POST /api/ask  — RAG endpoint
    // -------------------------------------------------------------------------

    @Test
    void askEndpointReturnsLlmResponseWhenDocumentsFound() throws Exception {
        Document doc = new Document(
                "Title: Insect Repellent Wristband\nPrice: $10.99\nCategory: Health & Wellness",
                Map.of("title", "Insect Repellent Wristband", "category", "Health & Wellness")
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        mockMvc.perform(post("/api/ask")
                        .param("query", "tell me about insect repellent wristband"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mocked LLM response"));
    }

    @Test
    void askEndpointReturnsLlmResponseWhenNoDocumentsFound() throws Exception {
        // Empty result — controller falls back to lastRetrievedContext (empty string initially)
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/ask")
                        .param("query", "something completely unrelated"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mocked LLM response"));
    }

    @Test
    void askEndpointReusesHistoryForFollowUpQuestion() throws Exception {
        Document doc = new Document(
                "Title: Insect Repellent Wristband\nPrice: $10.99",
                Map.of("title", "Insect Repellent Wristband", "category", "Health & Wellness")
        );

        // First call returns a document; second call (follow-up) returns nothing
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc))   // fresh question
                .thenReturn(List.of());     // follow-up question

        // Turn 1 — fresh question adds the product context to the history
        mockMvc.perform(post("/api/ask")
                        .param("query", "tell me about insect repellent wristband"))
                .andExpect(status().isOk());

        // Turn 2 — no new documents; the cached history still contains the wristband context
        mockMvc.perform(post("/api/ask")
                        .param("query", "what is the cost of it?"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mocked LLM response"));

        // LLM must be invoked for both turns — not short-circuited on the follow-up
        verify(ollamaChatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void askEndpointAccumulatesContextAcrossMultipleFreshQueries() throws Exception {
        // Three separate fresh queries, each returning a distinct product
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Title: Product One\nPrice: $10.00")))
                .thenReturn(List.of(new Document("Title: Product Two\nPrice: $20.00")))
                .thenReturn(List.of(new Document("Title: Product Three\nPrice: $30.00")))
                .thenReturn(List.of()); // follow-up returns nothing

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/ask").param("query", "tell me about product " + i))
                    .andExpect(status().isOk());
        }

        // Follow-up — no new docs; all three previously cached contexts are still available
        mockMvc.perform(post("/api/ask")
                        .param("query", "compare the prices of all the products you mentioned"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mocked LLM response"));

        // 3 fresh + 1 follow-up = 4 LLM calls total
        verify(ollamaChatModel, times(4)).call(any(Prompt.class));
    }

    @Test
    void askEndpointEvictsOldestContextWhenHistoryExceeds20() throws Exception {
        // Return a unique document for each of the first 21 calls, then nothing
        AtomicInteger callCount = new AtomicInteger(0);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenAnswer(invocation -> {
                    int n = callCount.incrementAndGet();
                    return n <= 21
                            ? List.of(new Document("Title: Product " + n + "\nPrice: $" + n + ".00"))
                            : List.of();
                });

        // 21 fresh queries — the 1st entry is evicted when the 21st is added
        for (int i = 1; i <= 21; i++) {
            mockMvc.perform(post("/api/ask").param("query", "product " + i))
                    .andExpect(status().isOk());
        }

        // A follow-up after eviction must still succeed (history holds entries 2–21)
        mockMvc.perform(post("/api/ask")
                        .param("query", "what were the prices?"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mocked LLM response"));

        // 21 fresh + 1 follow-up = 22 total LLM calls
        verify(ollamaChatModel, times(22)).call(any(Prompt.class));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a mocked {@link ChatResponse} that returns the given text from
     * {@code getResult().getOutput().getText()} and "mistral:latest" from
     * {@code getMetadata().getModel()}.
     */
    private ChatResponse buildMockChatResponse(String responseText) {
        AssistantMessage message = mock(AssistantMessage.class);
        when(message.getText()).thenReturn(responseText);

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(message);

        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        when(metadata.getModel()).thenReturn("mistral:latest");

        ChatResponse response = mock(ChatResponse.class);
        when(response.getResult()).thenReturn(generation);
        when(response.getResults()).thenReturn(List.of(generation));
        when(response.getMetadata()).thenReturn(metadata);

        return response;
    }
}
