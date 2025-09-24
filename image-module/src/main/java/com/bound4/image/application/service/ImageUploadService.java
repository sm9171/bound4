package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.DuplicateImageException;
import com.bound4.image.adapter.in.web.exception.ThumbnailGenerationException;
import com.bound4.image.application.port.in.ImageUploadUseCase;
import com.bound4.image.application.port.in.ThumbnailProcessingUseCase;
import com.bound4.image.application.port.in.UploadImageCommand;
import com.bound4.image.application.port.out.FileStorageService;
import com.bound4.image.application.port.out.HashService;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.FileHash;
import com.bound4.image.domain.Image;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ImageUploadService implements ImageUploadUseCase {
    
    private final ImageRepository imageRepository;
    private final ThumbnailProcessingUseCase thumbnailProcessingUseCase;
    private final HashService hashService;
    private final FileStorageService fileStorageService;
    
    public ImageUploadService(ImageRepository imageRepository, 
                             ThumbnailProcessingUseCase thumbnailProcessingUseCase,
                             HashService hashService,
                             FileStorageService fileStorageService) {
        this.imageRepository = imageRepository;
        this.thumbnailProcessingUseCase = thumbnailProcessingUseCase;
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
        
        // 5. 이미지 저장 (썸네일은 비동기로 처리)
        Image savedImage = imageRepository.save(image);
        
        // 6. 비동기 썸네일 생성 요청
        try {
            thumbnailProcessingUseCase.requestThumbnailGeneration(savedImage.getId());
        } catch (Exception e) {
            // 썸네일 생성 요청 실패는 로깅만 하고 업로드는 성공으로 처리
            // 나중에 수동으로 재시도 가능
            throw new ThumbnailGenerationException("Failed to request thumbnail generation for " + command.originalFilename(), e);
        }
        
        return savedImage;
    }
    
    private String generateStorageKey(Long projectId, String fileHash, String type) {
        return String.format("projects/%d/images/%s/%s_%s", projectId, type, fileHash, type);
    }
}