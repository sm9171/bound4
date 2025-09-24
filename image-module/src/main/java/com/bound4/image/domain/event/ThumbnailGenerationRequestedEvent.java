package com.bound4.image.domain.event;

import com.bound4.image.domain.ImageId;
import com.bound4.image.domain.ProjectId;

import java.time.LocalDateTime;

/**
 * 썸네일 생성 요청 이벤트
 * 이미지 업로드 완료 후 비동기 썸네일 생성을 위해 발행되는 도메인 이벤트
 */
public class ThumbnailGenerationRequestedEvent {
    
    private final ImageId imageId;
    private final ProjectId projectId;
    private final String originalImageKey;
    private final String originalFilename;
    private final String mimeType;
    private final LocalDateTime requestedAt;
    private final int retryCount;
    
    public ThumbnailGenerationRequestedEvent(ImageId imageId, ProjectId projectId, 
                                           String originalImageKey, String originalFilename, 
                                           String mimeType) {
        this(imageId, projectId, originalImageKey, originalFilename, mimeType, LocalDateTime.now(), 0);
    }
    
    public ThumbnailGenerationRequestedEvent(ImageId imageId, ProjectId projectId, 
                                           String originalImageKey, String originalFilename, 
                                           String mimeType, LocalDateTime requestedAt, int retryCount) {
        this.imageId = imageId;
        this.projectId = projectId;
        this.originalImageKey = originalImageKey;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.requestedAt = requestedAt;
        this.retryCount = retryCount;
    }
    
    public ThumbnailGenerationRequestedEvent withRetry() {
        return new ThumbnailGenerationRequestedEvent(
            imageId, projectId, originalImageKey, originalFilename, 
            mimeType, requestedAt, retryCount + 1
        );
    }
    
    public ImageId getImageId() {
        return imageId;
    }
    
    public ProjectId getProjectId() {
        return projectId;
    }
    
    public String getOriginalImageKey() {
        return originalImageKey;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    @Override
    public String toString() {
        return "ThumbnailGenerationRequestedEvent{" +
                "imageId=" + imageId +
                ", projectId=" + projectId +
                ", originalImageKey='" + originalImageKey + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", requestedAt=" + requestedAt +
                ", retryCount=" + retryCount +
                '}';
    }
}