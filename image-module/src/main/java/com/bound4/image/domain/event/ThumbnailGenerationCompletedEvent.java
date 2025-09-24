package com.bound4.image.domain.event;

import com.bound4.image.domain.ImageId;

import java.time.LocalDateTime;

/**
 * 썸네일 생성 완료 이벤트
 * 썸네일 생성 성공 시 발행되는 도메인 이벤트
 */
public class ThumbnailGenerationCompletedEvent {
    
    private final ImageId imageId;
    private final String thumbnailKey;
    private final long processingTimeMs;
    private final LocalDateTime completedAt;
    
    public ThumbnailGenerationCompletedEvent(ImageId imageId, String thumbnailKey, 
                                           long processingTimeMs, LocalDateTime completedAt) {
        this.imageId = imageId;
        this.thumbnailKey = thumbnailKey;
        this.processingTimeMs = processingTimeMs;
        this.completedAt = completedAt;
    }
    
    public static ThumbnailGenerationCompletedEvent of(ImageId imageId, String thumbnailKey, long processingTimeMs) {
        return new ThumbnailGenerationCompletedEvent(imageId, thumbnailKey, processingTimeMs, LocalDateTime.now());
    }
    
    public ImageId getImageId() {
        return imageId;
    }
    
    public String getThumbnailKey() {
        return thumbnailKey;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    @Override
    public String toString() {
        return "ThumbnailGenerationCompletedEvent{" +
                "imageId=" + imageId +
                ", thumbnailKey='" + thumbnailKey + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                ", completedAt=" + completedAt +
                '}';
    }
}