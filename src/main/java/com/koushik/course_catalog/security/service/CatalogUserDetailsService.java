package com.koushik.course_catalog.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.koushik.course_catalog.security.config.AppSecurityProperties;
import com.koushik.course_catalog.security.model.CatalogUserPrincipal;

@Service
public class CatalogUserDetailsService implements UserDetailsService {

    private final AppSecurityProperties securityProperties;

    public CatalogUserDetailsService(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return securityProperties.getUsers().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .map(user -> new CatalogUserPrincipal(
                        user.getUsername(),
                        user.getPassword(),
                        user.getDisplayName(),
                        user.getRole()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
