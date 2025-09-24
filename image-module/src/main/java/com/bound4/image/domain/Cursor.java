package com.bound4.image.domain;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class Cursor {
    private final Long id;
    private final LocalDateTime timestamp;
    private final String encodedValue;

    private Cursor(Long id, LocalDateTime timestamp, String encodedValue) {
        this.id = id;
        this.timestamp = timestamp;
        this.encodedValue = encodedValue;
    }

    public static Cursor of(Long id, LocalDateTime timestamp) {
        if (id == null || timestamp == null) {
            throw new IllegalArgumentException("ID and timestamp cannot be null");
        }
        
        String rawValue = id + ":" + timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String encodedValue = Base64.getEncoder().encodeToString(rawValue.getBytes(StandardCharsets.UTF_8));
        
        return new Cursor(id, timestamp, encodedValue);
    }

    public static Cursor fromEncoded(String encodedCursor) {
        if (encodedCursor == null || encodedCursor.trim().isEmpty()) {
            throw new IllegalArgumentException("Encoded cursor cannot be null or empty");
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(encodedCursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor format");
            }

            Long id = Long.parseLong(parts[0]);
            LocalDateTime timestamp = LocalDateTime.parse(parts[1], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            return new Cursor(id, timestamp, encodedCursor);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor format: " + e.getMessage(), e);
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getEncodedValue() {
        return encodedValue;
    }

    @Override
    public String toString() {
        return encodedValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Cursor cursor = (Cursor) obj;
        return encodedValue.equals(cursor.encodedValue);
    }

    @Override
    public int hashCode() {
        return encodedValue.hashCode();
    }
}