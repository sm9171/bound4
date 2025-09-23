package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.DuplicateImageException;
import com.bound4.image.adapter.in.web.exception.ThumbnailGenerationException;
import com.bound4.image.application.port.in.ImageUploadUseCase;
import com.bound4.image.application.port.in.UploadImageCommand;
import com.bound4.image.application.port.out.FileStorageService;
import com.bound4.image.application.port.out.HashService;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.application.port.out.ThumbnailService;
import com.bound4.image.domain.FileHash;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ImageUploadService implements ImageUploadUseCase {
    
    private final ImageRepository imageRepository;
    private final ThumbnailService thumbnailService;
    private final HashService hashService;
    private final FileStorageService fileStorageService;
    
    public ImageUploadService(ImageRepository imageRepository, 
                             ThumbnailService thumbnailService,
                             HashService hashService,
                             FileStorageService fileStorageService) {
        this.imageRepository = imageRepository;
        this.thumbnailService = thumbnailService;
        this.hashService = hashService;
        this.fileStorageService = fileStorageService;
    }
    
    @Override
    public List<Image> uploadImages(List<UploadImageCommand> commands) {
        List<Image> uploadedImages = new ArrayList<>();
        
        for (UploadImageCommand command : commands) {
            Image image = processImageUpload(command);
            uploadedImages.add(image);
        }
        
        return uploadedImages;
    }
    
    private Image processImageUpload(UploadImageCommand command) {
        // 1. 파일 해시 계산
        FileHash fileHash = hashService.calculateHash(command.data());
        
        // 2. 중복 검사
        Optional<Image> existingImage = imageRepository.findByHash(fileHash);
        if (existingImage.isPresent()) {
            throw new DuplicateImageException("Image with hash " + fileHash.value() + " already exists");
        }
        
        // 3. S3에 원본 이미지 업로드
        String originalImageKey = generateStorageKey(command.projectId().value(), fileHash.value(), "original");
        fileStorageService.uploadFile(originalImageKey, command.data(), command.mimeType());
        
        // 4. 이미지 객체 생성
        Image image = new Image(
            command.projectId(),
            command.originalFilename(),
            fileHash,
            command.data().length,
            command.mimeType(),
            originalImageKey
        );
        
        // 5. 썸네일 생성 및 S3 업로드
        try {
            ImageData imageData = ImageData.of(command.data());
            ImageData thumbnailData = thumbnailService.generateThumbnail(imageData, command.mimeType());
            
            String thumbnailKey = generateStorageKey(command.projectId().value(), fileHash.value(), "thumbnail");
            fileStorageService.uploadFile(thumbnailKey, thumbnailData.getData(), command.mimeType());
            
            image.setThumbnailKey(thumbnailKey);
        } catch (Exception e) {
            // 원본 파일 롤백
            fileStorageService.deleteFile(originalImageKey);
            throw new ThumbnailGenerationException("Failed to generate thumbnail for " + command.originalFilename(), e);
        }
        
        // 6. 저장
        return imageRepository.save(image);
    }
    
    private String generateStorageKey(Long projectId, String fileHash, String type) {
        return String.format("projects/%d/images/%s/%s_%s", projectId, type, fileHash, type);
    }
}