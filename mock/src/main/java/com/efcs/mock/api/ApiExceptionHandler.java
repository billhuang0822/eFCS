package com.efcs.pay.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(IllegalArgumentException exception) {
        return Map.of("error", "INVALID_MESSAGE", "message", exception.getMessage(), "timestamp", Instant.now().toString());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, Object> upstreamFailure(RuntimeException exception) {
        return Map.of("error", "UPSTREAM_FAILURE", "message", "Mock host request failed", "timestamp", Instant.now().toString());
    }
}
