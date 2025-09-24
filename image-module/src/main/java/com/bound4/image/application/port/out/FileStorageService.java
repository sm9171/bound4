package com.bound4.image.application.port.out;

import java.time.Duration;

public interface FileStorageService {
    
    String uploadFile(String key, byte[] fileData, String contentType);
    
    byte[] downloadFile(String key);
    
    void deleteFile(String key);
    
    String generatePresignedUrl(String key, Duration expiration);
    
    boolean fileExists(String key);
}