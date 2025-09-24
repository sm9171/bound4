package com.bound4.image.adapter.in.web.exception;

public class OptimisticLockException extends RuntimeException {
    
    public OptimisticLockException(String message) {
        super(message);
    }
    
    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}