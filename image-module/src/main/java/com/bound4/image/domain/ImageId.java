package com.bound4.image.domain;

public record ImageId(Long value) {
    public ImageId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ImageId must be positive");
        }
    }
    
    public static ImageId of(Long value) {
        return new ImageId(value);
    }
}