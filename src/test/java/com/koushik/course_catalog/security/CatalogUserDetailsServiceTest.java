package com.koushik.course_catalog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.koushik.course_catalog.security.config.AppSecurityProperties;
import com.koushik.course_catalog.security.service.CatalogUserDetailsService;

class CatalogUserDetailsServiceTest {

    private CatalogUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        AppSecurityProperties properties = new AppSecurityProperties();
        AppSecurityProperties.UserProperties admin = new AppSecurityProperties.UserProperties();
        admin.setUsername("admin");
        admin.setPassword(new BCryptPasswordEncoder(12).encode("Admin@123"));
        admin.setRole("ROLE_ADMIN");
        admin.setDisplayName("Administrator");
        properties.setUsers(List.of(admin));
        userDetailsService = new CatalogUserDetailsService(properties);
    }

    @Test
    void loadUserByUsername_returnsUserWithMatchingPassword() {
        var user = userDetailsService.loadUserByUsername("admin");
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(new BCryptPasswordEncoder(12).matches("Admin@123", user.getPassword())).isTrue();
    }

    @Test
    void loadUserByUsername_throwsWhenMissing() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
