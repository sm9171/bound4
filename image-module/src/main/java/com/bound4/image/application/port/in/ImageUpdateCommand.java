package com.bound4.image.application.port.in;

import com.bound4.image.domain.ImageStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImageUpdateCommand {
    
    private final Long imageId;
    private final Map<String, Object> tags;
    private final String memo;
    private final ImageStatus status;
    private final Long version;
    
    public ImageUpdateCommand(Long imageId, List<String> tags, String memo, 
                             ImageStatus status, Long version) {
        this.imageId = imageId;
        this.tags = tags != null ? tags.stream()
                .collect(Collectors.toMap(tag -> tag, tag -> true)) : null;
        this.memo = memo;
        this.status = status;
        this.version = version;
    }
    
    public Long getImageId() {
        return imageId;
    }
    
    public Map<String, Object> getTags() {
        return tags;
    }
    
    public String getMemo() {
        return memo;
    }
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public Long getVersion() {
        return version;
    }
}