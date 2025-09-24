package com.bound4.image.adapter.in.web.exception;

public class ImageAlreadyDeletedException extends RuntimeException {
    
    public ImageAlreadyDeletedException(Long imageId) {
        super("Image is already deleted with id: " + imageId);
    }
    
    public ImageAlreadyDeletedException(String message) {
        super(message);
    }
    
    public ImageAlreadyDeletedException(String message, Throwable cause) {
        super(message, cause);
    }
}