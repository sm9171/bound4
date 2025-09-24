package com.bound4.image.adapter.in.web;

import com.bound4.image.application.port.in.ThumbnailProcessingUseCase;
import com.bound4.image.domain.ImageId;
import com.bound4.image.domain.ThumbnailProcessingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 썸네일 처리 관련 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/thumbnails")
public class ThumbnailController {
    
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailController.class);
    
    private final ThumbnailProcessingUseCase thumbnailProcessingUseCase;
    
    public ThumbnailController(ThumbnailProcessingUseCase thumbnailProcessingUseCase) {
        this.thumbnailProcessingUseCase = thumbnailProcessingUseCase;
    }
    
    /**
     * 썸네일 생성 요청
     */
    @PostMapping("/images/{imageId}/generate")
    public ResponseEntity<ThumbnailProcessingResponse> requestThumbnailGeneration(@PathVariable Long imageId) {
        try {
            logger.info("Thumbnail generation request received for image: {}", imageId);
            
            thumbnailProcessingUseCase.requestThumbnailGeneration(ImageId.of(imageId));
            
            ThumbnailProcessingResponse response = ThumbnailProcessingResponse.success(
                "Thumbnail generation request submitted successfully"
            );
            
            return ResponseEntity.accepted().body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid thumbnail generation request for image: {}", imageId, e);
            return ResponseEntity.badRequest()
                    .body(ThumbnailProcessingResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing thumbnail generation request for image: {}", imageId, e);
            return ResponseEntity.internalServerError()
                    .body(ThumbnailProcessingResponse.error("Internal server error"));
        }
    }
    
    /**
     * 썸네일 처리 상태 조회
     */
    @GetMapping("/images/{imageId}/status")
    public ResponseEntity<ThumbnailStatusResponse> getThumbnailStatus(@PathVariable Long imageId) {
        try {
            logger.debug("Thumbnail status request for image: {}", imageId);
            
            ThumbnailProcessingStatus status = thumbnailProcessingUseCase.getThumbnailProcessingStatus(
                    ImageId.of(imageId));
            
            ThumbnailStatusResponse response = ThumbnailStatusResponse.success(status);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid thumbnail status request for image: {}", imageId, e);
            return ResponseEntity.badRequest()
                    .body(ThumbnailStatusResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving thumbnail status for image: {}", imageId, e);
            return ResponseEntity.internalServerError()
                    .body(ThumbnailStatusResponse.error("Internal server error"));
        }
    }
    
    /**
     * 썸네일 생성 재시도
     */
    @PostMapping("/images/{imageId}/retry")
    public ResponseEntity<ThumbnailProcessingResponse> retryThumbnailGeneration(@PathVariable Long imageId) {
        try {
            logger.info("Thumbnail generation retry request for image: {}", imageId);
            
            thumbnailProcessingUseCase.retryThumbnailGeneration(ImageId.of(imageId));
            
            ThumbnailProcessingResponse response = ThumbnailProcessingResponse.success(
                "Thumbnail generation retry request submitted successfully"
            );
            
            return ResponseEntity.accepted().body(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Invalid thumbnail retry request for image: {}", imageId, e);
            return ResponseEntity.badRequest()
                    .body(ThumbnailProcessingResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing thumbnail retry request for image: {}", imageId, e);
            return ResponseEntity.internalServerError()
                    .body(ThumbnailProcessingResponse.error("Internal server error"));
        }
    }
    
    /**
     * 썸네일 처리 응답 DTO
     */
    public static class ThumbnailProcessingResponse {
        private boolean success;
        private String message;
        
        public ThumbnailProcessingResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static ThumbnailProcessingResponse success(String message) {
            return new ThumbnailProcessingResponse(true, message);
        }
        
        public static ThumbnailProcessingResponse error(String message) {
            return new ThumbnailProcessingResponse(false, message);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * 썸네일 상태 응답 DTO
     */
    public static class ThumbnailStatusResponse {
        private boolean success;
        private ThumbnailStatusData data;
        private String message;
        
        public ThumbnailStatusResponse(boolean success, ThumbnailStatusData data, String message) {
            this.success = success;
            this.data = data;
            this.message = message;
        }
        
        public static ThumbnailStatusResponse success(ThumbnailProcessingStatus status) {
            ThumbnailStatusData data = new ThumbnailStatusData(
                status.getValue(),
                status.getDescription(),
                status.canRetry(),
                status.isCompleted(),
                status.isFailed(),
                status.isInProgress()
            );
            return new ThumbnailStatusResponse(true, data, null);
        }
        
        public static ThumbnailStatusResponse error(String message) {
            return new ThumbnailStatusResponse(false, null, message);
        }
        
        public boolean isSuccess() { return success; }
        public ThumbnailStatusData getData() { return data; }
        public String getMessage() { return message; }
        
        public static class ThumbnailStatusData {
            private String status;
            private String description;
            private boolean canRetry;
            private boolean isCompleted;
            private boolean isFailed;
            private boolean isInProgress;
            
            public ThumbnailStatusData(String status, String description, boolean canRetry, 
                                     boolean isCompleted, boolean isFailed, boolean isInProgress) {
                this.status = status;
                this.description = description;
                this.canRetry = canRetry;
                this.isCompleted = isCompleted;
                this.isFailed = isFailed;
                this.isInProgress = isInProgress;
            }
            
            public String getStatus() { return status; }
            public String getDescription() { return description; }
            public boolean isCanRetry() { return canRetry; }
            public boolean isCompleted() { return isCompleted; }
            public boolean isFailed() { return isFailed; }
            public boolean isInProgress() { return isInProgress; }
        }
    }
}