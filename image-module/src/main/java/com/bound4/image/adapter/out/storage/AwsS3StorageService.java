package com.bound4.image.adapter.out.storage;

import com.bound4.image.application.port.out.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Profile("prod")
public class AwsS3StorageService implements FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsS3StorageService.class);
    
    @Override
    public String uploadFile(String key, byte[] fileData, String contentType) {
        logger.info("AWS S3: Uploading file with key: {}, size: {} bytes", key, fileData.length);
        
        // TODO: AWS S3 SDK를 사용한 실제 구현
        // S3Client를 사용하여 PutObjectRequest 실행
        // 업로드 후 S3 URL 반환
        
        throw new UnsupportedOperationException("AWS S3 implementation not yet available");
    }
    
    @Override
    public byte[] downloadFile(String key) {
        logger.info("AWS S3: Downloading file with key: {}", key);
        
        // TODO: AWS S3 SDK를 사용한 실제 구현
        // S3Client를 사용하여 GetObjectRequest 실행
        // 파일 데이터를 byte[]로 반환
        
        throw new UnsupportedOperationException("AWS S3 implementation not yet available");
    }
    
    @Override
    public void deleteFile(String key) {
        logger.info("AWS S3: Deleting file with key: {}", key);
        
        // TODO: AWS S3 SDK를 사용한 실제 구현
        // S3Client를 사용하여 DeleteObjectRequest 실행
        
        throw new UnsupportedOperationException("AWS S3 implementation not yet available");
    }
    
    @Override
    public String generatePresignedUrl(String key, Duration expiration) {
        logger.info("AWS S3: Generating presigned URL for key: {}, expiration: {}", key, expiration);
        
        // TODO: AWS S3 SDK를 사용한 실제 구현
        // S3Presigner를 사용하여 presigned URL 생성
        // PresignGetObjectRequest 또는 PresignPutObjectRequest 사용
        
        throw new UnsupportedOperationException("AWS S3 implementation not yet available");
    }
    
    @Override
    public boolean fileExists(String key) {
        logger.info("AWS S3: Checking if file exists with key: {}", key);
        
        // TODO: AWS S3 SDK를 사용한 실제 구현
        // S3Client를 사용하여 HeadObjectRequest 실행
        // NoSuchKeyException 처리하여 존재 여부 확인
        
        throw new UnsupportedOperationException("AWS S3 implementation not yet available");
    }
}