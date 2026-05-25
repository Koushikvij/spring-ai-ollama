package com.koushik.springaicode.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String title;
    private String description;
    private String price;
    private String category;
    private String features;
}