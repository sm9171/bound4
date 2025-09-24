package com.bound4.image.application.service;

import com.bound4.image.application.port.in.ThumbnailProcessingUseCase;
import com.bound4.image.application.port.out.EventPublisher;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.*;
import com.bound4.image.domain.event.ThumbnailGenerationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 썸네일 처리 서비스
 * 비동기 썸네일 생성을 위한 이벤트 발행과 상태 관리를 담당
 */
@Service
@Transactional
public class ThumbnailProcessingService implements ThumbnailProcessingUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailProcessingService.class);
    
    private final ImageRepository imageRepository;
    private final EventPublisher eventPublisher;
    
    public ThumbnailProcessingService(ImageRepository imageRepository, EventPublisher eventPublisher) {
        this.imageRepository = imageRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public void requestThumbnailGeneration(ImageId imageId) {
        logger.info("Requesting thumbnail generation for image: {}", imageId.value());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId.value()));
        
        // 이미 썸네일이 생성되었거나 처리 중인 경우 스킵
        if (image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.COMPLETED ||
            image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.PROCESSING) {
            logger.info("Thumbnail generation already completed or in progress for image: {}", imageId.value());
            return;
        }
        
        // 썸네일 처리 상태를 PROCESSING으로 변경
        image.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.PROCESSING);
        imageRepository.save(image);
        
        // 썸네일 생성 요청 이벤트 발행
        ThumbnailGenerationRequestedEvent event = new ThumbnailGenerationRequestedEvent(
            image.getId(),
            image.getProjectId(),
            image.getOriginalImageKey(),
            image.getOriginalFilename(),
            image.getMimeType()
        );
        
        eventPublisher.publishThumbnailGenerationRequested(event);
        
        logger.info("Thumbnail generation event published for image: {}", imageId.value());
    }
    
    @Override
    @Transactional(readOnly = true)
    public ThumbnailProcessingStatus getThumbnailProcessingStatus(ImageId imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId.value()));
        
        return image.getThumbnailProcessingStatus();
    }
    
    @Override
    public void retryThumbnailGeneration(ImageId imageId) {
        logger.info("Retrying thumbnail generation for image: {}", imageId.value());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId.value()));
        
        // 재시도 가능한 상태인지 확인
        if (!image.getThumbnailProcessingStatus().canRetry()) {
            throw new IllegalStateException("Cannot retry thumbnail generation for image: " + imageId.value() + 
                    ", current status: " + image.getThumbnailProcessingStatus());
        }
        
        // 썸네일 처리 상태를 PROCESSING으로 변경
        image.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.PROCESSING);
        imageRepository.save(image);
        
        // 재시도를 위한 썸네일 생성 요청 이벤트 발행
        ThumbnailGenerationRequestedEvent event = new ThumbnailGenerationRequestedEvent(
            image.getId(),
            image.getProjectId(),
            image.getOriginalImageKey(),
            image.getOriginalFilename(),
            image.getMimeType()
        );
        
        eventPublisher.publishThumbnailGenerationRequested(event);
        
        logger.info("Thumbnail generation retry event published for image: {}", imageId.value());
    }
}