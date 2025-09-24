package com.bound4.image.domain;

public record FileHash(String value) {
    public FileHash {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("FileHash cannot be null or empty");
        }
        if (value.length() != 64) {
            throw new IllegalArgumentException("FileHash must be 64 characters (SHA-256)");
        }
        if (!value.matches("^[a-fA-F0-9]+$")) {
            throw new IllegalArgumentException("FileHash must be hexadecimal");
        }
    }
    
    public static FileHash of(String value) {
        return new FileHash(value);
    }
}