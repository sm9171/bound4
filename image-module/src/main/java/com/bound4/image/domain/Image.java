package com.bound4.image.domain;

import java.time.LocalDateTime;
import java.util.Map;

public class Image {
    private ImageId id;
    private final ProjectId projectId;
    private final String originalFilename;
    private final FileHash fileHash;
    private final long fileSize;
    private final String mimeType;
    private final ImageData imageData;
    private ImageData thumbnailData;
    private ImageStatus status;
    private Map<String, Object> tags;
    private String memo;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    
    public Image(ProjectId projectId, String originalFilename, FileHash fileHash, 
                 long fileSize, String mimeType, ImageData imageData) {
        this.projectId = projectId;
        this.originalFilename = originalFilename;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.imageData = imageData;
        this.status = ImageStatus.READY;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public ImageId getId() {
        return id;
    }
    
    public ProjectId getProjectId() {
        return projectId;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public FileHash getFileHash() {
        return fileHash;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public ImageData getImageData() {
        return imageData;
    }
    
    public ImageData getThumbnailData() {
        return thumbnailData;
    }
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public Map<String, Object> getTags() {
        return tags;
    }
    
    public String getMemo() {
        return memo;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    // For persistence
    public void setId(ImageId id) {
        this.id = id;
    }
}