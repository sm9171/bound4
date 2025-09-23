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
    private String originalImageKey;
    private String thumbnailKey;
    private ImageStatus status;
    private Map<String, Object> tags;
    private String memo;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;
    
    public Image(ProjectId projectId, String originalFilename, FileHash fileHash, 
                 long fileSize, String mimeType, String originalImageKey) {
        this.projectId = projectId;
        this.originalFilename = originalFilename;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.originalImageKey = originalImageKey;
        this.status = ImageStatus.READY;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(ImageStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTags(Map<String, Object> tags) {
        this.tags = tags;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateMemo(String memo) {
        this.memo = memo;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsDeleted() {
        this.status = ImageStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isDeleted() {
        return deletedAt != null;
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
    
    public String getOriginalImageKey() {
        return originalImageKey;
    }
    
    public String getThumbnailKey() {
        return thumbnailKey;
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
    
    public Long getVersion() {
        return version;
    }
    
    // For persistence
    public void setId(ImageId id) {
        this.id = id;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public void updatePartially(Map<String, Object> tags, String memo, ImageStatus status) {
        if (tags != null) {
            this.tags = tags;
        }
        if (memo != null) {
            this.memo = memo;
        }
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = LocalDateTime.now();
    }
}