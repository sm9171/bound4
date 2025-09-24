package com.bound4.image.adapter.in.web;

import com.bound4.image.application.port.in.ImageCursorListUseCase;
import com.bound4.image.domain.Cursor;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageStatus;
import com.bound4.image.domain.PageInfo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ImageCursorListResponse {
    
    private boolean success;
    private Data data;
    private String message;
    
    public ImageCursorListResponse(boolean success, Data data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }
    
    public static ImageCursorListResponse success(ImageCursorListUseCase.CursorPageResult<Image> result) {
        List<ImageItem> items = result.getContent().stream()
                .map(ImageItem::from)
                .toList();
                
        PageInfo pageInfo = result.getPageInfo();
        CursorPageable pageable = new CursorPageable(
            pageInfo.hasNext(),
            pageInfo.hasPrevious(),
            pageInfo.getNextCursor() != null ? pageInfo.getNextCursor().getEncodedValue() : null,
            pageInfo.getPreviousCursor() != null ? pageInfo.getPreviousCursor().getEncodedValue() : null,
            pageInfo.getSize(),
            pageInfo.getActualSize()
        );
        
        Data data = new Data(items, pageable);
        return new ImageCursorListResponse(true, data, null);
    }
    
    public static ImageCursorListResponse error(String message) {
        return new ImageCursorListResponse(false, null, message);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Data getData() {
        return data;
    }
    
    public String getMessage() {
        return message;
    }
    
    public static class Data {
        private List<ImageItem> content;
        private CursorPageable pageable;
        
        public Data(List<ImageItem> content, CursorPageable pageable) {
            this.content = content;
            this.pageable = pageable;
        }
        
        public List<ImageItem> getContent() {
            return content;
        }
        
        public CursorPageable getPageable() {
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
        private String cursor;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime updatedAt;
        
        public ImageItem(Long id, Long projectId, String filename, Long fileSize, 
                        String mimeType, ImageStatus status, List<String> tags, 
                        String memo, String cursor, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.projectId = projectId;
            this.filename = filename;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.status = status;
            this.tags = tags;
            this.memo = memo;
            this.cursor = cursor;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        public static ImageItem from(Image image) {
            Cursor cursor = Cursor.of(image.getId().value(), image.getCreatedAt());
            
            List<String> tags = image.getTags() != null ? 
                    image.getTags().keySet().stream().toList() : null;
            
            return new ImageItem(
                image.getId().value(),
                image.getProjectId().value(),
                image.getOriginalFilename(),
                image.getFileSize(),
                image.getMimeType(),
                image.getStatus(),
                tags,
                image.getMemo(),
                cursor.getEncodedValue(),
                image.getCreatedAt(),
                image.getUpdatedAt()
            );
        }
        
        // Getters
        public Long getId() { return id; }
        public Long getProjectId() { return projectId; }
        public String getFilename() { return filename; }
        public Long getFileSize() { return fileSize; }
        public String getMimeType() { return mimeType; }
        public ImageStatus getStatus() { return status; }
        public List<String> getTags() { return tags; }
        public String getMemo() { return memo; }
        public String getCursor() { return cursor; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }
    
    public static class CursorPageable {
        private boolean hasNext;
        private boolean hasPrevious;
        private String nextCursor;
        private String previousCursor;
        private int size;
        private int actualSize;
        
        public CursorPageable(boolean hasNext, boolean hasPrevious, String nextCursor, 
                            String previousCursor, int size, int actualSize) {
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
            this.nextCursor = nextCursor;
            this.previousCursor = previousCursor;
            this.size = size;
            this.actualSize = actualSize;
        }
        
        // Getters
        public boolean isHasNext() { return hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
        public String getNextCursor() { return nextCursor; }
        public String getPreviousCursor() { return previousCursor; }
        public int getSize() { return size; }
        public int getActualSize() { return actualSize; }
    }
}