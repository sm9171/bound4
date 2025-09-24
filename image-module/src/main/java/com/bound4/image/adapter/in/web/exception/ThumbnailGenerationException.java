package com.bound4.image.adapter.in.web.exception;

public class ThumbnailGenerationException extends RuntimeException {
    public ThumbnailGenerationException(String message) {
        super(message);
    }
    
    public ThumbnailGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}