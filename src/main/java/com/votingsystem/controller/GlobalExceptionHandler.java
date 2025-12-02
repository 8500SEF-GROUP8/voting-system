package com.votingsystem.controller;

import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String errorMessage = errors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining(", "));
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", errorMessage);
        response.put("error", "Validation Error");
        response.put("errors", errors);
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElementException(NoSuchElementException ex) {
        Map<String, String> response = new HashMap<>();
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "Requested resource not found";
        }
        response.put("message", message);
        response.put("error", "NoSuchElementException");
        
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<Map<String, String>> handleLazyInitializationException(LazyInitializationException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Database relationship loading error. Please try again.");
        response.put("error", "LazyInitializationException");
        response.put("details", ex.getMessage() != null ? ex.getMessage() : "Failed to load entity relationships");
        
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotWritableException(HttpMessageNotWritableException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Error serializing response. This may be due to unloaded entity relationships.");
        response.put("error", "SerializationError");
        response.put("details", ex.getMessage() != null ? ex.getMessage() : "Failed to serialize response");
        
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unexpected error occurred";
        }
        response.put("message", message);
        response.put("error", ex.getClass().getSimpleName());
        
        if (ex.getCause() != null) {
            response.put("cause", ex.getCause().getClass().getSimpleName() + ": " + ex.getCause().getMessage());
        }
        
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

