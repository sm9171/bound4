package com.bound4.image.adapter.in.web.exception;

public class DuplicateImageException extends RuntimeException {
    public DuplicateImageException(String message) {
        super(message);
    }
    
    public DuplicateImageException(String message, Throwable cause) {
        super(message, cause);
    }
}