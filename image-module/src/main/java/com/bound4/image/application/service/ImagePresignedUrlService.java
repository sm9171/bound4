package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.ImageNotFoundException;
import com.bound4.image.application.port.in.ImagePresignedUrlQuery;
import com.bound4.image.application.port.in.ImagePresignedUrlUseCase;
import com.bound4.image.application.port.out.FileStorageService;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImagePresignedUrlService implements ImagePresignedUrlUseCase {
    
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    
    public ImagePresignedUrlService(ImageRepository imageRepository, FileStorageService fileStorageService) {
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
    }
    
    @Override
    public String generatePresignedUrl(ImagePresignedUrlQuery query) {
        ImageId imageId = ImageId.of(query.getImageId());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(query.getImageId()));
        
        String storageKey;
        
        switch (query.getDataType()) {
            case ORIGINAL:
                storageKey = image.getOriginalImageKey();
                break;
            case THUMBNAIL:
                storageKey = image.getThumbnailKey();
                if (storageKey == null) {
                    throw new IllegalStateException("Thumbnail not available for image: " + query.getImageId());
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid image data type: " + query.getDataType());
        }
        
        return fileStorageService.generatePresignedUrl(storageKey, query.getExpiration());
    }
}