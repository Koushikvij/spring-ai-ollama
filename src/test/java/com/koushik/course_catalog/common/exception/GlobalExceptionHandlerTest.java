package com.koushik.course_catalog.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.koushik.course_catalog.common.saga.SagaException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<Object> response = handler.handleNotFound(
                new ResourceNotFoundException("Course not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().toString()).contains("Course not found");
    }

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<Object> response = handler.handleBadRequest(
                new BadRequestException("Invalid input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleSagaFailure_returns409() {
        ResponseEntity<Object> response = handler.handleSagaFailure(
                new SagaException("Saga [EnrollCustomer] failed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

}
