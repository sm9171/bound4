package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.ImageAlreadyDeletedException;
import com.bound4.image.adapter.in.web.exception.ImageNotFoundException;
import com.bound4.image.application.port.in.ImageDeleteCommand;
import com.bound4.image.application.port.in.ImageDeleteUseCase;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImageDeleteService implements ImageDeleteUseCase {
    
    private final ImageRepository imageRepository;
    
    public ImageDeleteService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }
    
    @Override
    public Image deleteImage(ImageDeleteCommand command) {
        ImageId imageId = ImageId.of(command.getImageId());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(command.getImageId()));
        
        if (image.isDeleted()) {
            throw new ImageAlreadyDeletedException(command.getImageId());
        }
        
        image.markAsDeleted();
        
        return imageRepository.save(image);
    }
}