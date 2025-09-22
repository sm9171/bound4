package com.bound4.image.adapter.in.web;

import com.bound4.image.adapter.in.web.exception.DuplicateImageException;
import com.bound4.image.adapter.in.web.exception.ThumbnailGenerationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DuplicateImageException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateImageException(DuplicateImageException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(ThumbnailGenerationException.class)
    public ResponseEntity<ApiResponse<Void>> handleThumbnailGenerationException(ThumbnailGenerationException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Failed to read uploaded files: " + e.getMessage()));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("No files provided"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
    }
}