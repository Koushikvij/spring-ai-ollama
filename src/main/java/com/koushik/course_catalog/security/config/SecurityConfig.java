package com.koushik.course_catalog.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import com.koushik.course_catalog.security.filter.JsonAuthenticationFilter;
import com.koushik.course_catalog.security.filter.RoleAuthorizationFilter;
import com.koushik.course_catalog.security.handler.SecurityHandlers;
import com.koushik.course_catalog.security.service.CatalogUserDetailsService;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(
            CatalogUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    JsonAuthenticationFilter jsonAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityHandlers securityHandlers) {
        JsonAuthenticationFilter filter = new JsonAuthenticationFilter(authenticationManager);
        filter.setAuthenticationSuccessHandler(securityHandlers.jsonAuthenticationSuccessHandler());
        filter.setAuthenticationFailureHandler(securityHandlers.jsonAuthenticationFailureHandler());
        return filter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JsonAuthenticationFilter jsonAuthenticationFilter,
            RoleAuthorizationFilter roleAuthorizationFilter,
            CatalogUserDetailsService userDetailsService,
            PersistentTokenRepository persistentTokenRepository,
            SecurityHandlers securityHandlers) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fix -> fix.migrateSession())
                        .maximumSessions(1))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .rememberMe(remember -> remember
                        .userDetailsService(userDetailsService)
                        .tokenRepository(persistentTokenRepository)
                        .tokenValiditySeconds(604800)
                        .rememberMeParameter("rememberMe")
                        .rememberMeCookieName("CATALOG_REMEMBER_ME"))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler(securityHandlers.jsonLogoutSuccessHandler())
                        .deleteCookies("CATALOG_SESSION", "CATALOG_REMEMBER_ME", "JSESSIONID"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Access denied\"}");
                        }))
                .addFilterAt(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(roleAuthorizationFilter, SecurityContextHolderFilter.class);

        return http.build();
    }
}
