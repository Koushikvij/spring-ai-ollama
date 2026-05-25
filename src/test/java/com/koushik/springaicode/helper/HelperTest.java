package com.koushik.springaicode.helper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Helper#extract(String, String)}.
 *
 * No Spring context needed — Helper is a plain Java class.
 */
class HelperTest {

    private final Helper helper = new Helper();

    @Test
    void extractsQuotedTitle() {
        String input = "\"Bluetooth Wireless Earbuds\"";
        assertThat(helper.extract(input, "\"(.*?)\""))
                .isEqualTo("Bluetooth Wireless Earbuds");
    }

    @Test
    void extractsDescriptionValue() {
        String input = "Description: Noise-canceling earbuds with 30 hours battery.";
        assertThat(helper.extract(input, "Description:\\s*(.*)"))
                .isEqualTo("Noise-canceling earbuds with 30 hours battery.");
    }

    @Test
    void extractsPriceValue() {
        String input = "Price: $49.99";
        assertThat(helper.extract(input, "Price:\\s*(.*)"))
                .isEqualTo("$49.99");
    }

    @Test
    void extractsCategoryValue() {
        String input = "Category: Electronics";
        assertThat(helper.extract(input, "Category:\\s*(.*)"))
                .isEqualTo("Electronics");
    }

    @Test
    void extractsFeaturesValue() {
        String input = "Features: Touch controls, Charging case, Bluetooth 5.3";
        assertThat(helper.extract(input, "Features:\\s*(.*)"))
                .isEqualTo("Touch controls, Charging case, Bluetooth 5.3");
    }

    @Test
    void returnsEmptyStringWhenPatternDoesNotMatch() {
        String input = "Title: \"Some Product\"";
        assertThat(helper.extract(input, "Description:\\s*(.*)"))
                .isEmpty();
    }

    @Test
    void trimsSurroundingWhitespace() {
        String input = "Price:    $19.99   ";
        assertThat(helper.extract(input, "Price:\\s*(.*)"))
                .isEqualTo("$19.99");
    }
}
