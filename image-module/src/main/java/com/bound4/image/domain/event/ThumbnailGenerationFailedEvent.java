package com.bound4.image.domain.event;

import com.bound4.image.domain.ImageId;

import java.time.LocalDateTime;

/**
 * 썸네일 생성 실패 이벤트
 * 썸네일 생성 실패 시 발행되는 도메인 이벤트
 */
public class ThumbnailGenerationFailedEvent {
    
    private final ImageId imageId;
    private final String errorMessage;
    private final String errorType;
    private final int retryCount;
    private final boolean canRetry;
    private final LocalDateTime failedAt;
    
    public ThumbnailGenerationFailedEvent(ImageId imageId, String errorMessage, String errorType, 
                                        int retryCount, boolean canRetry, LocalDateTime failedAt) {
        this.imageId = imageId;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.retryCount = retryCount;
        this.canRetry = canRetry;
        this.failedAt = failedAt;
    }
    
    public static ThumbnailGenerationFailedEvent of(ImageId imageId, Exception exception, 
                                                   int retryCount, boolean canRetry) {
        return new ThumbnailGenerationFailedEvent(
            imageId,
            exception.getMessage(),
            exception.getClass().getSimpleName(),
            retryCount,
            canRetry,
            LocalDateTime.now()
        );
    }
    
    public ImageId getImageId() {
        return imageId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public boolean canRetry() {
        return canRetry;
    }
    
    public LocalDateTime getFailedAt() {
        return failedAt;
    }
    
    @Override
    public String toString() {
        return "ThumbnailGenerationFailedEvent{" +
                "imageId=" + imageId +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorType='" + errorType + '\'' +
                ", retryCount=" + retryCount +
                ", canRetry=" + canRetry +
                ", failedAt=" + failedAt +
                '}';
    }
}