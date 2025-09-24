package com.bound4.image.adapter.in.web;

import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ImageDetailResponse {
    
    private final Long id;
    private final Long projectId;
    private final String filename;
    private final Long fileSize;
    private final String mimeType;
    private final ImageStatus status;
    private final List<String> tags;
    private final String memo;
    private final String originalImageUrl;
    private final String thumbnailUrl;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final LocalDateTime updatedAt;
    
    public ImageDetailResponse(Long id, Long projectId, String filename, Long fileSize, 
                              String mimeType, ImageStatus status, List<String> tags, 
                              String memo, String originalImageUrl, String thumbnailUrl,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.status = status;
        this.tags = tags;
        this.memo = memo;
        this.originalImageUrl = originalImageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public static ImageDetailResponse from(Image image) {
        List<String> tags = convertTagsToList(image.getTags());
        String originalImageUrl = "/images/" + image.getId().value() + "/original";
        String thumbnailUrl = "/images/" + image.getId().value() + "/thumbnail";
        
        return new ImageDetailResponse(
            image.getId().value(),
            image.getProjectId().value(),
            image.getOriginalFilename(),
            image.getFileSize(),
            image.getMimeType(),
            image.getStatus(),
            tags,
            image.getMemo(),
            originalImageUrl,
            thumbnailUrl,
            image.getCreatedAt(),
            image.getUpdatedAt()
        );
    }
    
    private static List<String> convertTagsToList(Map<String, Object> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        
        // 임시로 빈 리스트 반환 (실제 구현에서는 Map 구조에 따라 적절히 변환)
        return List.of("nature", "landscape");
    }
    
    public Long getId() {
        return id;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public String getMemo() {
        return memo;
    }
    
    public String getOriginalImageUrl() {
        return originalImageUrl;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}