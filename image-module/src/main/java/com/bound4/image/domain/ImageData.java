package com.bound4.image.domain;

import java.util.Arrays;

public record ImageData(byte[] data) {
    public ImageData {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
    }
    
    public static ImageData of(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
        return new ImageData(data.clone());
    }
    
    public byte[] getData() {
        return data.clone();
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImageData imageData = (ImageData) obj;
        return Arrays.equals(data, imageData.data);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}