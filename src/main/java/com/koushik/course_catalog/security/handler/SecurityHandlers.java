package com.koushik.course_catalog.security.handler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koushik.course_catalog.security.dto.AuthResponse;
import com.koushik.course_catalog.security.model.CatalogUserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityHandlers {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthenticationSuccessHandler jsonAuthenticationSuccessHandler() {
        return (request, response, authentication) -> writeAuthResponse(response, authentication, HttpStatus.OK,
                "Login successful");
    }

    public AuthenticationFailureHandler jsonAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "message", "Invalid username or password"));
        };
    }

    public LogoutSuccessHandler jsonLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "message", "Logout successful"));
        };
    }

    public void writeAuthResponse(
            HttpServletResponse response,
            Authentication authentication,
            HttpStatus status,
            String message) throws IOException {
        CatalogUserPrincipal principal = (CatalogUserPrincipal) authentication.getPrincipal();
        AuthResponse body = new AuthResponse(
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getRole(),
                message);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
