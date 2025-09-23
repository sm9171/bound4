package com.bound4.image.adapter.in.web;

import com.bound4.image.adapter.out.persistence.ImageQueryRepository;
import com.bound4.image.application.port.in.ImageListQuery;
import com.bound4.image.application.port.in.ImageListUseCase;
import com.bound4.image.application.port.in.ImageUploadUseCase;
import com.bound4.image.application.port.in.UploadImageCommand;
import com.bound4.image.domain.Image;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/images")
public class ImageController {
    
    private final ImageUploadUseCase imageUploadUseCase;
    private final ImageListUseCase imageListUseCase;
    
    public ImageController(ImageUploadUseCase imageUploadUseCase, ImageListUseCase imageListUseCase) {
        this.imageUploadUseCase = imageUploadUseCase;
        this.imageListUseCase = imageListUseCase;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadImages(
            @PathVariable Long projectId,
            @RequestParam("files") MultipartFile[] files) throws IOException {
        
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files provided");
        }
        
        ImageUploadRequest request = new ImageUploadRequest(projectId, files);
        List<UploadImageCommand> commands = request.toCommands();
        
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("No valid files provided");
        }
        
        List<Image> uploadedImages = imageUploadUseCase.uploadImages(commands);
        List<ImageResponse> responses = uploadedImages.stream()
            .map(ImageResponse::from)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping
    public ResponseEntity<ImageListResponse> getImages(
            @PathVariable Long projectId,
            @ModelAttribute ImageListRequest request) {
        
        int validatedPage = Math.max(0, request.getPage());
        int validatedSize = Math.min(100, Math.max(1, request.getSize()));
        
        List<String> tags = null;
        if (request.getTags() != null && !request.getTags().trim().isEmpty()) {
            tags = Arrays.asList(request.getTags().split(","));
        }
        
        ImageListQuery query = new ImageListQuery(
            projectId,
            validatedPage,
            validatedSize,
            request.getStatus(),
            tags,
            request.getSort(),
            request.getDirection()
        );
        
        Page<ImageQueryRepository.ImageListProjection> page = imageListUseCase.getImageList(query);
        
        List<ImageListResponse.ImageItem> imageItems = page.getContent().stream()
            .map(projection -> new ImageListResponse.ImageItem(
                projection.getId(),
                projection.getProjectId(),
                projection.getFilename(),
                projection.getFileSize(),
                projection.getMimeType(),
                projection.getStatus(),
                projection.getTags(),
                projection.getMemo(),
                projection.getCreatedAt()
            ))
            .toList();
        
        ImageListResponse.Pageable pageable = new ImageListResponse.Pageable(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
        
        ImageListResponse.Data data = new ImageListResponse.Data(imageItems, pageable);
        
        return ResponseEntity.ok(new ImageListResponse(true, data));
    }
}