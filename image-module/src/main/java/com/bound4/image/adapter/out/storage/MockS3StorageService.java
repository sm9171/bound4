package com.bound4.image.adapter.out.storage;

import com.bound4.image.application.port.out.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("!prod")
public class MockS3StorageService implements FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockS3StorageService.class);
    private final ConcurrentHashMap<String, MockS3Object> storage = new ConcurrentHashMap<>();
    private static final String MOCK_BUCKET = "mock-s3-bucket";
    private static final String MOCK_BASE_URL = "https://mock-s3.amazonaws.com";
    
    @Override
    public String uploadFile(String key, byte[] fileData, String contentType) {
        logger.info("Mock S3: Uploading file with key: {}, size: {} bytes", key, fileData.length);
        
        MockS3Object s3Object = MockS3Object.builder()
                .key(key)
                .data(fileData)
                .contentType(contentType)
                .uploadTime(LocalDateTime.now())
                .build();
        
        storage.put(key, s3Object);
        
        String url = generateMockUrl(key);
        logger.info("Mock S3: File uploaded successfully. URL: {}", url);
        return url;
    }
    
    @Override
    public byte[] downloadFile(String key) {
        logger.info("Mock S3: Downloading file with key: {}", key);
        
        MockS3Object s3Object = storage.get(key);
        if (s3Object == null) {
            throw new RuntimeException("File not found: " + key);
        }
        
        logger.info("Mock S3: File downloaded successfully. Size: {} bytes", s3Object.getSize());
        return s3Object.getData();
    }
    
    @Override
    public void deleteFile(String key) {
        logger.info("Mock S3: Deleting file with key: {}", key);
        
        MockS3Object removedObject = storage.remove(key);
        if (removedObject == null) {
            logger.warn("Mock S3: File not found for deletion: {}", key);
        } else {
            logger.info("Mock S3: File deleted successfully: {}", key);
        }
    }
    
    @Override
    public String generatePresignedUrl(String key, Duration expiration) {
        logger.info("Mock S3: Generating presigned URL for key: {}, expiration: {}", key, expiration);
        
        long expirationTimestamp = System.currentTimeMillis() + expiration.toMillis();
        String presignedUrl = String.format("%s/%s/%s?X-Amz-Expires=%d&X-Amz-Signature=mock-signature&expires=%d",
                MOCK_BASE_URL, MOCK_BUCKET, key, expiration.getSeconds(), expirationTimestamp);
        
        logger.info("Mock S3: Generated presigned URL: {}", presignedUrl);
        return presignedUrl;
    }
    
    @Override
    public boolean fileExists(String key) {
        boolean exists = storage.containsKey(key);
        logger.info("Mock S3: File exists check for key: {} -> {}", key, exists);
        return exists;
    }
    
    private String generateMockUrl(String key) {
        return String.format("%s/%s/%s", MOCK_BASE_URL, MOCK_BUCKET, key);
    }
    
    public int getStorageSize() {
        return storage.size();
    }
    
    public void clearStorage() {
        logger.info("Mock S3: Clearing all storage");
        storage.clear();
    }
}