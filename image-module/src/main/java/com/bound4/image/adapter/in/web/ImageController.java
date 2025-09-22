package com.bound4.image.adapter.in.web;

import com.bound4.image.application.port.in.ImageUploadUseCase;
import com.bound4.image.application.port.in.UploadImageCommand;
import com.bound4.image.domain.Image;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/images")
public class ImageController {
    
    private final ImageUploadUseCase imageUploadUseCase;
    
    public ImageController(ImageUploadUseCase imageUploadUseCase) {
        this.imageUploadUseCase = imageUploadUseCase;
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
}