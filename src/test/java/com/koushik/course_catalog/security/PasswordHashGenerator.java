package com.koushik.course_catalog.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Run once to print BCrypt (strength 12) hashes for application-users.yml.
 */
public final class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String[] passwords = {"Admin@123", "Manager@123", "Employee@123", "Customer@123"};
        for (String password : passwords) {
            System.out.println(password + " -> " + encoder.encode(password));
        }
    }
}
