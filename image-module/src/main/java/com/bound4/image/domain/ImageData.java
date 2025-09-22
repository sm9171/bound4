package com.bound4.image.domain;

public record ImageData(byte[] data) {
    public ImageData {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
    }
    
    public static ImageData of(byte[] data) {
        return new ImageData(data.clone());
    }
    
    public byte[] getData() {
        return data.clone();
    }
    
    public long getSize() {
        return data.length;
    }
}