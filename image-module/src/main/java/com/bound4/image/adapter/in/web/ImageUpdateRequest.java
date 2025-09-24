package com.bound4.image.adapter.in.web;

import com.bound4.image.domain.ImageStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageUpdateRequest {
    
    private List<String> tags;
    
    @Size(max = 1000, message = "Memo must not exceed 1000 characters")
    private String memo;
    
    private ImageStatus status;
    
    @NotNull(message = "Version is required for optimistic locking")
    private Long version;
    
    public ImageUpdateRequest() {
    }
    
    public ImageUpdateRequest(List<String> tags, String memo, ImageStatus status, Long version) {
        this.tags = tags;
        this.memo = memo;
        this.status = status;
        this.version = version;
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
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public void setStatus(ImageStatus status) {
        this.status = status;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}