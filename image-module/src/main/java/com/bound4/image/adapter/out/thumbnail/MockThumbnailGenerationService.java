package com.bound4.image.adapter.out.thumbnail;

import com.bound4.image.application.port.out.ThumbnailGenerationService;
import com.bound4.image.domain.ImageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Mock 썸네일 생성 서비스 (개발/테스트용)
 */
@Service
@Profile("!prod")
public class MockThumbnailGenerationService implements ThumbnailGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockThumbnailGenerationService.class);
    
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
        "image/jpeg",
        "image/jpg", 
        "image/png",
        "image/gif",
        "image/webp"
    );
    
    @Override
    public String generateThumbnail(ImageId imageId, String originalImageKey, String mimeType) 
            throws ThumbnailGenerationException {
        
        logger.info("Mock thumbnail generation for image: {}, originalKey: {}, mimeType: {}", 
                   imageId.value(), originalImageKey, mimeType);
        
        // MIME 타입 검증
        if (!isSupported(mimeType)) {
            throw new ThumbnailGenerationException(
                "Unsupported MIME type: " + mimeType, false);
        }
        
        try {
            // 실제 썸네일 생성을 시뮬레이션하기 위한 지연
            Thread.sleep(100 + (long) (Math.random() * 500)); // 100-600ms 랜덤 지연
            
            // 10% 확률로 재시도 가능한 실패 시뮬레이션
            if (Math.random() < 0.1) {
                throw new ThumbnailGenerationException(
                    "Simulated temporary failure for testing", true);
            }
            
            // 2% 확률로 영구 실패 시뮬레이션
            if (Math.random() < 0.02) {
                throw new ThumbnailGenerationException(
                    "Simulated permanent failure for testing", false);
            }
            
            // 성공적인 썸네일 생성 시뮬레이션
            String thumbnailKey = "thumbnails/" + imageId.value() + "_" + UUID.randomUUID() + "_thumb.jpg";
            
            logger.info("Mock thumbnail generated successfully: {}", thumbnailKey);
            return thumbnailKey;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThumbnailGenerationException(
                "Thumbnail generation interrupted", true);
        }
    }
    
    @Override
    public boolean isSupported(String mimeType) {
        return SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase());
    }
}