package com.bound4.image.adapter.in.web;

import com.bound4.image.application.port.in.*;
import com.bound4.image.domain.Image;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/images")
public class ImageDetailController {
    
    private final ImageDetailUseCase imageDetailUseCase;
    private final ImageDataUseCase imageDataUseCase;
    private final ImageUpdateUseCase imageUpdateUseCase;
    private final ImageDeleteUseCase imageDeleteUseCase;
    
    public ImageDetailController(ImageDetailUseCase imageDetailUseCase, 
                                ImageDataUseCase imageDataUseCase,
                                ImageUpdateUseCase imageUpdateUseCase,
                                ImageDeleteUseCase imageDeleteUseCase) {
        this.imageDetailUseCase = imageDetailUseCase;
        this.imageDataUseCase = imageDataUseCase;
        this.imageUpdateUseCase = imageUpdateUseCase;
        this.imageDeleteUseCase = imageDeleteUseCase;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImageDetailResponse>> getImageDetail(@PathVariable Long id) {
        ImageDetailQuery query = new ImageDetailQuery(id);
        Image image = imageDetailUseCase.getImageDetail(query);
        
        ImageDetailResponse response = ImageDetailResponse.from(image);
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body(ApiResponse.success(response));
    }
    
    @GetMapping("/{id}/original")
    public ResponseEntity<byte[]> getOriginalImage(@PathVariable Long id) {
        ImageDataQuery query = new ImageDataQuery(id, ImageDataQuery.ImageDataType.ORIGINAL);
        ImageDataUseCase.ImageDataResponse response = imageDataUseCase.getImageData(query);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.getMimeType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "inline; filename=\"" + response.getFilename() + "\"")
                .body(response.getImageData().getData());
    }
    
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnailImage(@PathVariable Long id) {
        ImageDataQuery query = new ImageDataQuery(id, ImageDataQuery.ImageDataType.THUMBNAIL);
        ImageDataUseCase.ImageDataResponse response = imageDataUseCase.getImageData(query);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.getMimeType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "inline; filename=\"" + response.getFilename() + "\"")
                .body(response.getImageData().getData());
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ImageUpdateResponse>> updateImage(
            @PathVariable Long id,
            @Valid @RequestBody ImageUpdateRequest request) {
        
        ImageUpdateCommand command = new ImageUpdateCommand(
            id,
            request.getTags(),
            request.getMemo(),
            request.getStatus(),
            request.getVersion()
        );
        
        Image updatedImage = imageUpdateUseCase.updateImage(command);
        ImageUpdateResponse response = ImageUpdateResponse.from(updatedImage);
        
        return ResponseEntity.ok()
                .body(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ImageDeleteResponse>> deleteImage(@PathVariable Long id) {
        ImageDeleteCommand command = new ImageDeleteCommand(id);
        Image deletedImage = imageDeleteUseCase.deleteImage(command);
        
        ImageDeleteResponse response = ImageDeleteResponse.from(deletedImage);
        
        return ResponseEntity.ok()
                .body(ApiResponse.success(response, "이미지가 성공적으로 삭제되었습니다."));
    }
}