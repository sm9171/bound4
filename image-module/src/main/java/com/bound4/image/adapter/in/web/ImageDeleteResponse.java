package com.bound4.image.adapter.in.web;

import com.bound4.image.domain.Image;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ImageDeleteResponse {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime deletedAt;
    
    public ImageDeleteResponse() {
    }
    
    public ImageDeleteResponse(Long id, LocalDateTime deletedAt) {
        this.id = id;
        this.deletedAt = deletedAt;
    }
    
    public static ImageDeleteResponse from(Image image) {
        return new ImageDeleteResponse(
            image.getId().value(),
            image.getDeletedAt()
        );
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}