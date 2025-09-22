package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.DuplicateImageException;
import com.bound4.image.adapter.in.web.exception.ThumbnailGenerationException;
import com.bound4.image.application.port.in.ImageUploadUseCase;
import com.bound4.image.application.port.in.UploadImageCommand;
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
    
    public ImageUploadService(ImageRepository imageRepository, 
                             ThumbnailService thumbnailService,
                             HashService hashService) {
        this.imageRepository = imageRepository;
        this.thumbnailService = thumbnailService;
        this.hashService = hashService;
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
        
        // 3. 이미지 객체 생성
        ImageData imageData = ImageData.of(command.data());
        Image image = new Image(
            command.projectId(),
            command.originalFilename(),
            fileHash,
            command.data().length,
            command.mimeType(),
            imageData
        );
        
        // 4. 썸네일 생성
        try {
            ImageData thumbnailData = thumbnailService.generateThumbnail(imageData, command.mimeType());
            image.setThumbnail(thumbnailData);
        } catch (Exception e) {
            throw new ThumbnailGenerationException("Failed to generate thumbnail for " + command.originalFilename(), e);
        }
        
        // 5. 저장
        return imageRepository.save(image);
    }
}