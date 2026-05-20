package com.koushik.course_catalog.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashGeneratorTest {

    @Test
    void printBcryptHashesForConfig() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String[] passwords = {"Admin@123", "Manager@123", "Employee@123", "Customer@123"};
        for (String password : passwords) {
            System.out.println(password + " -> " + encoder.encode(password));
        }
    }
}
