package com.bound4.image.adapter.in.web;

import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ImageUpdateResponse {
    
    private Long id;
    private Long projectId;
    private String filename;
    private ImageStatus status;
    private List<String> tags;
    private String memo;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;
    
    private Long version;
    
    public ImageUpdateResponse() {
    }
    
    public ImageUpdateResponse(Long id, Long projectId, String filename, 
                              ImageStatus status, List<String> tags, String memo, 
                              LocalDateTime updatedAt, Long version) {
        this.id = id;
        this.projectId = projectId;
        this.filename = filename;
        this.status = status;
        this.tags = tags;
        this.memo = memo;
        this.updatedAt = updatedAt;
        this.version = version;
    }
    
    public static ImageUpdateResponse from(Image image) {
        List<String> tagList = null;
        if (image.getTags() != null) {
            tagList = image.getTags().keySet().stream()
                    .toList();
        }
        
        return new ImageUpdateResponse(
            image.getId().value(),
            image.getProjectId().value(),
            image.getOriginalFilename(),
            image.getStatus(),
            tagList,
            image.getMemo(),
            image.getUpdatedAt(),
            image.getVersion()
        );
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public void setStatus(ImageStatus status) {
        this.status = status;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public String getMemo() {
        return memo;
    }
    
    public void setMemo(String memo) {
        this.memo = memo;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}