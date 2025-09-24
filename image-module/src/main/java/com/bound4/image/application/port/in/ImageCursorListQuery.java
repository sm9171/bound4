package com.bound4.image.application.port.in;

import com.bound4.image.domain.Cursor;
import com.bound4.image.domain.ImageStatus;
import com.bound4.image.domain.SortDirection;

import java.util.List;

public class ImageCursorListQuery {
    
    private final Long projectId;
    private final Cursor cursor;
    private final int size;
    private final SortDirection direction;
    private final ImageStatus status;
    private final List<String> tags;
    private final String sortBy;
    
    public ImageCursorListQuery(Long projectId, Cursor cursor, int size, SortDirection direction,
                               ImageStatus status, List<String> tags, String sortBy) {
        this.projectId = projectId;
        this.cursor = cursor;
        this.size = size;
        this.direction = direction != null ? direction : SortDirection.DESC;
        this.status = status;
        this.tags = tags;
        this.sortBy = sortBy != null ? sortBy : "createdAt";
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public Cursor getCursor() {
        return cursor;
    }
    
    public int getSize() {
        return size;
    }
    
    public SortDirection getDirection() {
        return direction;
    }
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public boolean hasAfterCursor() {
        return cursor != null && direction == SortDirection.DESC;
    }
    
    public boolean hasBeforeCursor() {
        return cursor != null && direction == SortDirection.ASC;
    }
    
    public static class Builder {
        private Long projectId;
        private Cursor cursor;
        private int size = 20; // 기본값
        private SortDirection direction = SortDirection.DESC;
        private ImageStatus status;
        private List<String> tags;
        private String sortBy = "createdAt";
        
        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }
        
        public Builder cursor(Cursor cursor) {
            this.cursor = cursor;
            return this;
        }
        
        public Builder size(int size) {
            if (size <= 0 || size > 100) {
                throw new IllegalArgumentException("Size must be between 1 and 100");
            }
            this.size = size;
            return this;
        }
        
        public Builder direction(SortDirection direction) {
            this.direction = direction;
            return this;
        }
        
        public Builder status(ImageStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }
        
        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }
        
        public ImageCursorListQuery build() {
            if (projectId == null) {
                throw new IllegalArgumentException("ProjectId is required");
            }
            return new ImageCursorListQuery(projectId, cursor, size, direction, status, tags, sortBy);
        }
    }
}