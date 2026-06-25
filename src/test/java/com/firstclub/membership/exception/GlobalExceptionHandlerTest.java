package com.firstclub.membership.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects; // Import this

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should correctly serialize ResourceNotFoundException parameters down to HTTP 404 block structures")
    void handleNotFound_ShouldReturnStructured404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Entity not found reference");
        
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        Map<String, Object> body = Objects.requireNonNull(response.getBody(), "Response body must not be null");
        
        assertThat(body.get("message")).isEqualTo("Entity not found reference");
        assertThat(body.get("timestamp").toString()).endsWith("Z"); 
    }

    @Test
    @DisplayName("Should catch unexpected generic runtime exceptions and output sanitized 500 Server payloads")
    void handleUnhandledExceptions_ShouldSanitizeSecretData() {
        NullPointerException ex = new NullPointerException("Root system hardware pointer fault simulation");

        ResponseEntity<Map<String, Object>> response = handler.handleUnhandledExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        Map<String, Object> body = Objects.requireNonNull(response.getBody(), "Response body must not be null");
        
        assertThat(body.get("message"))
                .isEqualTo("An unexpected internal error occurred. Please contact system administrators.");
    }
}