package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.ImageNotFoundException;
import com.bound4.image.application.port.in.ImageDetailQuery;
import com.bound4.image.application.port.in.ImageDetailUseCase;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImageDetailService implements ImageDetailUseCase {
    
    private final ImageRepository imageRepository;
    
    public ImageDetailService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }
    
    @Override
    public Image getImageDetail(ImageDetailQuery query) {
        ImageId imageId = ImageId.of(query.getImageId());
        
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(query.getImageId()));
    }
}