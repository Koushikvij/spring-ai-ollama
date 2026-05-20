package com.koushik.course_catalog.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String username;
    private String displayName;
    private String role;
    private String message;
}
