package com.koushik.springaicode.controller;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import java.util.stream.Collectors;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koushik.springaicode.service.EmbeddingService;


@RestController
public class OllamaController {
    private static final Logger logger = LoggerFactory.getLogger(OllamaController.class);
    
    private final ChatClient chatClient;
    private final ChatMemory chatMemory =
            MessageWindowChatMemory.builder().build();

    private final EmbeddingService embeddingService;

    private final VectorStore vectorStore;

    // Rolling history of the last MAX_CONTEXT_HISTORY vector-search results.
    // Each entry is the raw product text retrieved for one fresh query.
    // Follow-up questions reuse the full history so the model can answer
    // questions like "compare the prices of all the products you mentioned."
    private static final int MAX_CONTEXT_HISTORY = 20;
    private final Deque<String> contextHistory = new ArrayDeque<>();

    public OllamaController(OllamaChatModel ollamaChatModel, EmbeddingService embeddingService, VectorStore vectorStore) {

        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;

        this.chatClient = ChatClient.builder(ollamaChatModel)
                .defaultOptions(ChatOptions.builder()
                        .model("mistral:latest")
                        .build())
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @GetMapping("/api/{message}")
    public ResponseEntity<String> getAnswer(@PathVariable String message) {
        ChatResponse chatResponse = chatClient.prompt(message)
                .call()
                .chatResponse();

        logger.info("Model used: {}", chatResponse.getMetadata().getModel());

        String response = chatResponse
                .getResult()
                .getOutput()
                .getText();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/recommend")
    public String recommend(@RequestParam String year, @RequestParam String genre, @RequestParam String language) {
        String template = """
        You are a movie recommendation assistant.
        I want to watch a {genre} movie with good rating from the year {year} in {language}. 
        Suggest one movie that fits these criteria and provide a brief description of the plot and provide the cast as well.

        Response format:
        Movie Title: <title>
        Description: <brief description of the plot>
        Cast: <main actors/actresses>
        Length: <duration of the movie>
        Director: <director's name>
        IMDB Rating: <rating>

                """;
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of("genre", genre, "year", year, "language", language));
        String response = chatClient.prompt(prompt)
                .call().content();

        return response;
    }
    
    @PostMapping("/api/embedding")
    public ResponseEntity<float[]> getEmbedding(@RequestParam String text) {
        logger.info("Embedding text: {}", text);
        float[] vector = embeddingService.embed(text);
        logger.debug("Vector length = {}", vector.length);
        return ResponseEntity.ok(vector);
    }

    @PostMapping("/api/similarity")
    public double getSimilarity(@RequestParam String text1, @RequestParam String text2) {
        logger.info("Calculating similarity for texts: {} and {}", text1, text2);
        float[] embedding1 = embeddingService.embed(text1);
        float[] embedding2 = embeddingService.embed(text2);
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i]; 
        }
        logger.debug("Similarity for texts: {} and {}: {}", text1, text2, dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @PostMapping("/api/product")
    public List<Document> getProduct(@RequestParam String text) {
        logger.info("Searching for product with text: {}", text);

        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(text)
                .topK(5)
                .similarityThreshold(0.6)
                .build());

    }

    @PostMapping("/api/ask")
    public String getResultswithRAG(@RequestParam String query) {

        logger.info("Received query for RAG: {}", query);

        // Retrieve relevant documents from the vector store
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.6)
                .build());

        if (!docs.isEmpty()) {
            // Fresh question — add the new context to the rolling history
            String newContext = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));

            if (contextHistory.size() >= MAX_CONTEXT_HISTORY) {
                contextHistory.pollFirst(); // evict the oldest entry
            }
            contextHistory.addLast(newContext);
            logger.info("Vector search returned {} document(s) — history size: {}/{}.",
                    docs.size(), contextHistory.size(), MAX_CONTEXT_HISTORY);
        } else {
            // Follow-up question — history is unchanged; all prior contexts remain available
            logger.info("Vector search returned no documents — using {} cached context(s) for follow-up.",
                    contextHistory.size());
        }

        // Combine the full history so the model can answer questions that span
        // multiple previous products (e.g. "compare the prices of both items").
        String combinedContext = String.join("\n\n---\n\n", contextHistory);

        return chatClient
                .prompt()
                .system("""
                        You are a product assistant.
                        Answer ONLY using the provided product context below.
                        If the context does not contain the answer, say:
                        "I don't have enough information in the product catalog."

                        Context:
                        """ + combinedContext)
                .user(query)
                .call()
                .content();
    }
    
}

