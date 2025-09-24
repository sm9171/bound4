package com.bound4.image.adapter.in.web.exception;

public class ImageNotFoundException extends RuntimeException {
    
    public ImageNotFoundException(Long imageId) {
        super("Image not found with id: " + imageId);
    }
}