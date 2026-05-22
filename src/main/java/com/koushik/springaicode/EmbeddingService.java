package com.koushik.springaicode;

import java.util.List;
import java.util.Map;


import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
            "model", "nomic-embed-text",
            "prompt", text.toLowerCase()
        );

        Map<String, Object> response = restTemplate.postForObject(
            "http://localhost:11434/api/embeddings", body, Map.class
        );

        List<Double> embedding = (List<Double>) response.get("embedding");

        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }
}