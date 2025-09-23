package com.bound4.image.application.port.in;

import com.bound4.image.domain.ImageStatus;

import java.util.List;

public class ImageListQuery {
    
    private final Long projectId;
    private final int page;
    private final int size;
    private final ImageStatus status;
    private final List<String> tags;
    private final String sort;
    private final String direction;
    
    public ImageListQuery(Long projectId, int page, int size, ImageStatus status, 
                         List<String> tags, String sort, String direction) {
        this.projectId = projectId;
        this.page = page;
        this.size = size;
        this.status = status;
        this.tags = tags;
        this.sort = sort;
        this.direction = direction;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public int getPage() {
        return page;
    }
    
    public int getSize() {
        return size;
    }
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public String getSort() {
        return sort;
    }
    
    public String getDirection() {
        return direction;
    }
}