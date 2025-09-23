package com.bound4.image.adapter.in.web;

import com.bound4.image.domain.ImageStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public class ImageListResponse {
    
    private boolean success;
    private Data data;
    
    public ImageListResponse(boolean success, Data data) {
        this.success = success;
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Data getData() {
        return data;
    }
    
    public static class Data {
        private List<ImageItem> content;
        private Pageable pageable;
        
        public Data(List<ImageItem> content, Pageable pageable) {
            this.content = content;
            this.pageable = pageable;
        }
        
        public List<ImageItem> getContent() {
            return content;
        }
        
        public Pageable getPageable() {
            return pageable;
        }
    }
    
    public static class ImageItem {
        private Long id;
        private Long projectId;
        private String filename;
        private Long fileSize;
        private String mimeType;
        private ImageStatus status;
        private List<String> tags;
        private String memo;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime createdAt;
        
        public ImageItem(Long id, Long projectId, String filename, Long fileSize, 
                        String mimeType, ImageStatus status, List<String> tags, 
                        String memo, LocalDateTime createdAt) {
            this.id = id;
            this.projectId = projectId;
            this.filename = filename;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.status = status;
            this.tags = tags;
            this.memo = memo;
            this.createdAt = createdAt;
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
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
    
    public static class Pageable {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        
        public Pageable(int page, int size, long totalElements, int totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }
        
        public int getPage() {
            return page;
        }
        
        public int getSize() {
            return size;
        }
        
        public long getTotalElements() {
            return totalElements;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
    }
}