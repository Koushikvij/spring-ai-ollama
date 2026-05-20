package com.koushik.course_catalog.security.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koushik.course_catalog.security.dto.AuthResponse;
import com.koushik.course_catalog.security.model.CatalogUserPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @GetMapping("me")
    public ResponseEntity<AuthResponse> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CatalogUserPrincipal principal = (CatalogUserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new AuthResponse(
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getRole(),
                "Authenticated"));
    }
}
