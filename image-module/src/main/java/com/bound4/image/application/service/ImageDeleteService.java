package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.ImageAlreadyDeletedException;
import com.bound4.image.adapter.in.web.exception.ImageNotFoundException;
import com.bound4.image.application.port.in.ImageDeleteCommand;
import com.bound4.image.application.port.in.ImageDeleteUseCase;
import com.bound4.image.application.port.out.FileStorageService;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImageDeleteService implements ImageDeleteUseCase {
    
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    
    public ImageDeleteService(ImageRepository imageRepository, FileStorageService fileStorageService) {
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
    }
    
    @Override
    public Image deleteImage(ImageDeleteCommand command) {
        ImageId imageId = ImageId.of(command.getImageId());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(command.getImageId()));
        
        if (image.isDeleted()) {
            throw new ImageAlreadyDeletedException(command.getImageId());
        }
        
        // S3에서 파일 삭제
        try {
            if (image.getOriginalImageKey() != null) {
                fileStorageService.deleteFile(image.getOriginalImageKey());
            }
            if (image.getThumbnailKey() != null) {
                fileStorageService.deleteFile(image.getThumbnailKey());
            }
        } catch (Exception e) {
            // S3 삭제 실패해도 DB 삭제는 진행 (로그만 남김)
            // 실제 운영환경에서는 별도의 정리 작업이나 재시도 로직 필요
        }
        
        image.markAsDeleted();
        
        return imageRepository.save(image);
    }
}