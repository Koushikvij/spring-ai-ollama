package com.koushik.springaicode.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.koushik.springaicode.entity.Product;
import com.koushik.springaicode.helper.Helper;

import jakarta.annotation.PostConstruct;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;

@Component
public class DataInitializer {

    @Autowired
    private VectorStore vectorStore;

    @PostConstruct
    public void initData() throws IOException {
        Resource resource = new ClassPathResource("product_details.txt");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // TextReader reader = new TextReader(new ClassPathResource("product_details.txt"));
        // TokenTextSplitter splitter = new TokenTextSplitter(100,20,5,1000,false);
        // List<Document> documents = splitter.split(reader.get());
        // List<Document> documents = reader.get();

        // String content = documents.stream()
        //     .map(Document::getText)
        //     .reduce("", (a, b) -> a + "\n" + b);

        List<Product> products = parseProducts(content);
        List<Document> docs = products.stream()
            .map(p -> new Document(
                buildText(p),
                Map.of(
                    "title", p.getTitle(),
                    "category", p.getCategory()
                )
            ))
            .toList();

        vectorStore.add(docs);
    }
    
    public List<Product> parseProducts(String content) {
        List<Product> products = new ArrayList<>();
        Helper helper = new Helper();
        String[] blocks = content.split("(?m)^Title:");

        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;

            Product product = new Product();

            product.setTitle(helper.extract(block, "\"(.*?)\""));
            product.setDescription(helper.extract(block, "Description:\\s*(.*)"));
            product.setPrice(helper.extract(block, "Price:\\s*(.*)"));
            product.setCategory(helper.extract(block, "Category:\\s*(.*)"));
            product.setFeatures(helper.extract(block, "Features:\\s*(.*)"));

            products.add(product);
        }

        return products;
    }

    private String buildText(Product p) {
        return """
            Title: %s
            Description: %s
            Price: %s
            Category: %s
            Features: %s
            """.formatted(
            p.getTitle(),
            p.getDescription(),
            p.getPrice(),
            p.getCategory(),
            p.getFeatures()
        );
    }
}
