package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.ImageNotFoundException;
import com.bound4.image.application.port.in.ImageDataQuery;
import com.bound4.image.application.port.in.ImageDataUseCase;
import com.bound4.image.application.port.out.FileStorageService;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageData;
import com.bound4.image.domain.ImageId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImageDataService implements ImageDataUseCase {
    
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    
    public ImageDataService(ImageRepository imageRepository, FileStorageService fileStorageService) {
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
    }
    
    @Override
    public ImageDataResponse getImageData(ImageDataQuery query) {
        ImageId imageId = ImageId.of(query.getImageId());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(query.getImageId()));
        
        String storageKey;
        String filename;
        
        switch (query.getDataType()) {
            case ORIGINAL:
                storageKey = image.getOriginalImageKey();
                filename = image.getOriginalFilename();
                break;
            case THUMBNAIL:
                storageKey = image.getThumbnailKey();
                if (storageKey == null) {
                    throw new IllegalStateException("Thumbnail not available for image: " + query.getImageId());
                }
                filename = "thumb_" + image.getOriginalFilename();
                break;
            default:
                throw new IllegalArgumentException("Invalid image data type: " + query.getDataType());
        }
        
        // S3에서 이미지 데이터 다운로드
        byte[] imageBytes = fileStorageService.downloadFile(storageKey);
        ImageData imageData = ImageData.of(imageBytes);
        
        return new ImageDataResponse(imageData, image.getMimeType(), filename);
    }
}