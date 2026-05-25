package com.koushik.springaicode.config;

import com.koushik.springaicode.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DataInitializer}.
 *
 * Uses Mockito only — no Spring context required.
 * Mocks are injected via field injection to match the @Autowired fields
 * in DataInitializer.
 */
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DataInitializer dataInitializer;

    // -------------------------------------------------------------------------
    // parseProducts()
    // -------------------------------------------------------------------------

    @Test
    void parseProductsParsesAllFieldsCorrectly() {
        String content = """
                Title: "Test Headphones"
                Description: Great sound quality for all occasions.
                Price: $49.99
                Category: Electronics
                Features: Wireless, Noise-cancelling, USB-C
                """;

        List<Product> products = dataInitializer.parseProducts(content);

        assertThat(products).hasSize(1);
        Product p = products.get(0);
        assertThat(p.getTitle()).isEqualTo("Test Headphones");
        assertThat(p.getDescription()).isEqualTo("Great sound quality for all occasions.");
        assertThat(p.getPrice()).isEqualTo("$49.99");
        assertThat(p.getCategory()).isEqualTo("Electronics");
        assertThat(p.getFeatures()).isEqualTo("Wireless, Noise-cancelling, USB-C");
    }

    @Test
    void parseProductsHandlesMultipleProducts() {
        String content = """
                Title: "Product One"
                Description: First product description.
                Price: $10.00
                Category: Category A
                Features: Feature1, Feature2

                Title: "Product Two"
                Description: Second product description.
                Price: $20.00
                Category: Category B
                Features: Feature3, Feature4
                """;

        List<Product> products = dataInitializer.parseProducts(content);

        assertThat(products).hasSize(2);
        assertThat(products.get(0).getTitle()).isEqualTo("Product One");
        assertThat(products.get(1).getTitle()).isEqualTo("Product Two");
    }

    @Test
    void parseProductsSkipsLeadingEmptyBlock() {
        // Content that starts with a newline before the first Title:
        String content = "\n\nTitle: \"Single Product\"\nDescription: A product.\nPrice: $5.00\nCategory: Cat\nFeatures: F1";

        List<Product> products = dataInitializer.parseProducts(content);

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getTitle()).isEqualTo("Single Product");
    }

    @Test
    void parseProductsReturnsEmptyListForBlankContent() {
        List<Product> products = dataInitializer.parseProducts("   \n\n   ");
        assertThat(products).isEmpty();
    }

    // -------------------------------------------------------------------------
    // initData() idempotency
    // -------------------------------------------------------------------------

    @Test
    void initDataSkipsInsertionWhenVectorStoreAlreadyHasData() throws IOException {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(200);

        dataInitializer.initData();

        // vectorStore.add() must never be called when data already exists
        verify(vectorStore, never()).add(any());
    }

    @Test
    void initDataInsertsProductsWhenVectorStoreIsEmpty() throws IOException {
        // Return 0 so DataInitializer proceeds with insertion
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        dataInitializer.initData();

        // vectorStore.add() must have been called with the parsed documents
        verify(vectorStore).add(any());
    }
}
