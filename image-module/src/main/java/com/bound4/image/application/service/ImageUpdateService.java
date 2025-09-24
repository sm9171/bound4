package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.ImageNotFoundException;
import com.bound4.image.application.port.in.ImageUpdateCommand;
import com.bound4.image.application.port.in.ImageUpdateUseCase;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImageUpdateService implements ImageUpdateUseCase {
    
    private final ImageRepository imageRepository;
    
    public ImageUpdateService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }
    
    @Override
    public Image updateImage(ImageUpdateCommand command) {
        ImageId imageId = ImageId.of(command.getImageId());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(command.getImageId()));
        
        if (!command.getVersion().equals(image.getVersion())) {
            throw new OptimisticLockingFailureException(
                "Image has been modified by another process. Expected version: " + 
                command.getVersion() + ", but was: " + image.getVersion());
        }
        
        image.updatePartially(command.getTags(), command.getMemo(), command.getStatus());
        
        return imageRepository.save(image);
    }
}