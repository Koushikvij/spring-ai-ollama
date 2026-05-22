package com.koushik.springaicode;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.media.ArraySchema;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class OllamaController {
    private static final Logger logger = LoggerFactory.getLogger(OllamaController.class);
    
    private final ChatClient chatClient;
    private final ChatMemory chatMemory =
            MessageWindowChatMemory.builder().build();

    private final EmbeddingService embeddingService;
    // public OllamaController(OllamaChatModel chatModel) {
    //     this.chatClient = ChatClient.create(chatModel);
    // }

    public OllamaController(OllamaChatModel ollamaChatModel,  EmbeddingService embeddingService) {

        this.embeddingService = embeddingService;

        this.chatClient = ChatClient.builder(ollamaChatModel)
                .defaultOptions(ChatOptions.builder()
                        .model("gpt-oss")
                        .build())
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

//    public OllamaController(ChatClient.Builder builder) {
//         ChatModel chatModel = OllamaChatModel.builder().modelManagementOptions(OllamaChatModel.ModelManagementOptions.builder()
//                 .model("deepseek-r1:latest")
//                 .build())
//                 .build();
//         builder = ChatClient.builder("deepseek-r1:latest");
//         this.chatClient = builder
//                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory)
//                        .build())
//                .build();

//    }

    @GetMapping("/api/{message}")
    public ResponseEntity<String> getAnswer(@PathVariable String message) {
        ChatResponse chatResponse = chatClient.prompt(message)
                .call()
                .chatResponse();

        System.out.println(chatResponse.getMetadata().getModel());


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
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}

