package com.bound4.image.domain;

public record ProjectId(Long value) {
    public ProjectId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ProjectId must be positive");
        }
    }
    
    public static ProjectId of(Long value) {
        return new ProjectId(value);
    }
}