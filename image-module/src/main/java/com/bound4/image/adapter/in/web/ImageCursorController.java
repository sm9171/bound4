package com.bound4.image.adapter.in.web;

import com.bound4.image.application.port.in.ImageCursorListQuery;
import com.bound4.image.application.port.in.ImageCursorListUseCase;
import com.bound4.image.application.port.in.ImageListQuery;
import com.bound4.image.domain.Cursor;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageStatus;
import com.bound4.image.domain.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v2/images")
public class ImageCursorController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageCursorController.class);
    
    private final ImageCursorListUseCase imageCursorListUseCase;
    
    public ImageCursorController(ImageCursorListUseCase imageCursorListUseCase) {
        this.imageCursorListUseCase = imageCursorListUseCase;
    }
    
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ImageCursorListResponse> getImagesCursorBased(
            @PathVariable Long projectId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tags) {
        
        try {
            logger.info("Cursor-based image list request for project: {}, cursor: {}, size: {}", 
                       projectId, cursor, size);
            
            ImageCursorListQuery.Builder queryBuilder = ImageCursorListQuery.builder()
                    .projectId(projectId)
                    .size(size)
                    .direction(SortDirection.fromString(direction))
                    .sortBy(sortBy);
            
            if (cursor != null && !cursor.trim().isEmpty()) {
                try {
                    Cursor parsedCursor = Cursor.fromEncoded(cursor);
                    queryBuilder.cursor(parsedCursor);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid cursor format: {}", cursor, e);
                    return ResponseEntity.badRequest()
                            .body(ImageCursorListResponse.error("Invalid cursor format"));
                }
            }
            
            if (status != null && !status.trim().isEmpty()) {
                try {
                    ImageStatus imageStatus = ImageStatus.valueOf(status.toUpperCase());
                    queryBuilder.status(imageStatus);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(ImageCursorListResponse.error("Invalid status: " + status));
                }
            }
            
            if (tags != null && !tags.trim().isEmpty()) {
                List<String> tagList = Arrays.asList(tags.split(","));
                queryBuilder.tags(tagList);
            }
            
            ImageCursorListQuery query = queryBuilder.build();
            ImageCursorListUseCase.CursorPageResult<Image> result = imageCursorListUseCase.getImagesCursorBased(query);
            
            return ResponseEntity.ok(ImageCursorListResponse.success(result));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for project: {}", projectId, e);
            return ResponseEntity.badRequest()
                    .body(ImageCursorListResponse.error("Invalid request parameters: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching images for project: {}", projectId, e);
            return ResponseEntity.internalServerError()
                    .body(ImageCursorListResponse.error("Internal server error"));
        }
    }
    
    @PostMapping("/projects/{projectId}/performance-comparison")
    public ResponseEntity<PagingPerformanceResponse> comparePerformance(
            @PathVariable Long projectId,
            @RequestBody PerformanceComparisonRequest request) {
        
        try {
            logger.info("Performance comparison request for project: {}", projectId);
            
            // Offset 쿼리 생성
            ImageListQuery offsetQuery = new ImageListQuery(
                projectId,
                request.getOffsetPage(),
                request.getSize(),
                request.getStatus(),
                request.getTags(),
                request.getSortBy(),
                request.getDirection()
            );
            
            // Cursor 쿼리 생성
            ImageCursorListQuery.Builder cursorBuilder = ImageCursorListQuery.builder()
                    .projectId(projectId)
                    .size(request.getSize())
                    .direction(SortDirection.fromString(request.getDirection()))
                    .sortBy(request.getSortBy())
                    .status(request.getStatus())
                    .tags(request.getTags());
            
            if (request.getCursor() != null && !request.getCursor().trim().isEmpty()) {
                Cursor cursor = Cursor.fromEncoded(request.getCursor());
                cursorBuilder.cursor(cursor);
            }
            
            ImageCursorListQuery cursorQuery = cursorBuilder.build();
            
            ImageCursorListUseCase.PagingPerformanceMetrics metrics = 
                    imageCursorListUseCase.comparePerformance(offsetQuery, cursorQuery);
            
            return ResponseEntity.ok(PagingPerformanceResponse.success(metrics));
            
        } catch (Exception e) {
            logger.error("Error comparing pagination performance for project: {}", projectId, e);
            return ResponseEntity.internalServerError()
                    .body(PagingPerformanceResponse.error("Performance comparison failed: " + e.getMessage()));
        }
    }
    
    public static class PerformanceComparisonRequest {
        private int offsetPage;
        private String cursor;
        private int size;
        private String direction;
        private String sortBy;
        private ImageStatus status;
        private List<String> tags;
        
        // Constructors
        public PerformanceComparisonRequest() {}
        
        public PerformanceComparisonRequest(int offsetPage, String cursor, int size, String direction, 
                                          String sortBy, ImageStatus status, List<String> tags) {
            this.offsetPage = offsetPage;
            this.cursor = cursor;
            this.size = size;
            this.direction = direction;
            this.sortBy = sortBy;
            this.status = status;
            this.tags = tags;
        }
        
        // Getters and Setters
        public int getOffsetPage() { return offsetPage; }
        public void setOffsetPage(int offsetPage) { this.offsetPage = offsetPage; }
        
        public String getCursor() { return cursor; }
        public void setCursor(String cursor) { this.cursor = cursor; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
        public ImageStatus getStatus() { return status; }
        public void setStatus(ImageStatus status) { this.status = status; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
}