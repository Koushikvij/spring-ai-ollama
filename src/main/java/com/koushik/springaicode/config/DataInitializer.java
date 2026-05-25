package com.koushik.springaicode.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.koushik.springaicode.entity.Product;
import com.koushik.springaicode.helper.Helper;

import jakarta.annotation.PostConstruct;

@Component
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initData() throws IOException {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
        if (count != null && count > 0) {
            logger.info("Vector store already contains {} documents — skipping initialisation.", count);
            return;
        }

        Resource resource = new ClassPathResource("product_details.txt");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

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
        logger.info("Inserted {} products into the vector store.", docs.size());
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
