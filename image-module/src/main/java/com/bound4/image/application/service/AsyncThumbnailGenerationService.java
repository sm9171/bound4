package com.bound4.image.application.service;

import com.bound4.image.application.port.out.EventPublisher;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.application.port.out.ThumbnailGenerationService;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.RetryStrategy;
import com.bound4.image.domain.ThumbnailProcessingStatus;
import com.bound4.image.domain.event.ThumbnailGenerationCompletedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationFailedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 비동기 썸네일 생성 서비스
 * 이벤트를 수신하여 실제 썸네일 생성 작업을 수행하고 결과를 처리
 */
@Service
public class AsyncThumbnailGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncThumbnailGenerationService.class);
    
    private final ImageRepository imageRepository;
    private final ThumbnailGenerationService thumbnailGenerationService;
    private final EventPublisher eventPublisher;
    private final RetryStrategy retryStrategy;
    
    public AsyncThumbnailGenerationService(ImageRepository imageRepository,
                                         ThumbnailGenerationService thumbnailGenerationService,
                                         EventPublisher eventPublisher) {
        this.imageRepository = imageRepository;
        this.thumbnailGenerationService = thumbnailGenerationService;
        this.eventPublisher = eventPublisher;
        this.retryStrategy = new RetryStrategy();
    }
    
    /**
     * 썸네일 생성 요청 이벤트 처리
     */
    @Async("thumbnailTaskExecutor")
    @EventListener
    @Transactional
    public CompletableFuture<Void> handleThumbnailGenerationRequested(ThumbnailGenerationRequestedEvent event) {
        logger.info("Processing thumbnail generation request for image: {}, retry count: {}", 
                   event.getImageId().value(), event.getRetryCount());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 이미지 존재 여부 확인
            Optional<Image> imageOpt = imageRepository.findById(event.getImageId());
            if (imageOpt.isEmpty()) {
                logger.warn("Image not found for thumbnail generation: {}", event.getImageId().value());
                return CompletableFuture.completedFuture(null);
            }
            
            Image image = imageOpt.get();
            
            // 썸네일 생성 지원 여부 확인
            if (!thumbnailGenerationService.isSupported(event.getMimeType())) {
                logger.warn("Thumbnail generation not supported for MIME type: {} for image: {}", 
                           event.getMimeType(), event.getImageId().value());
                
                image.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.FAILED_PERMANENT);
                imageRepository.save(image);
                
                ThumbnailGenerationFailedEvent failedEvent = ThumbnailGenerationFailedEvent.of(
                    event.getImageId(),
                    new UnsupportedOperationException("Unsupported MIME type: " + event.getMimeType()),
                    event.getRetryCount(),
                    false
                );
                eventPublisher.publishThumbnailGenerationFailed(failedEvent);
                return CompletableFuture.completedFuture(null);
            }
            
            // 썸네일 생성 실행
            String thumbnailKey = thumbnailGenerationService.generateThumbnail(
                event.getImageId(),
                event.getOriginalImageKey(),
                event.getMimeType()
            );
            
            // 성공 시 이미지 업데이트
            image.setThumbnailKey(thumbnailKey); // 이미 COMPLETED 상태로 변경됨
            imageRepository.save(image);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // 성공 이벤트 발행
            ThumbnailGenerationCompletedEvent completedEvent = ThumbnailGenerationCompletedEvent.of(
                event.getImageId(),
                thumbnailKey,
                processingTime
            );
            eventPublisher.publishThumbnailGenerationCompleted(completedEvent);
            
            logger.info("Thumbnail generation completed for image: {} in {}ms", 
                       event.getImageId().value(), processingTime);
            
        } catch (ThumbnailGenerationService.ThumbnailGenerationException e) {
            logger.error("Thumbnail generation failed for image: {}, retry count: {}, error: {}", 
                        event.getImageId().value(), event.getRetryCount(), e.getMessage(), e);
            
            handleThumbnailGenerationFailure(event, e);
            
        } catch (Exception e) {
            logger.error("Unexpected error during thumbnail generation for image: {}, retry count: {}", 
                        event.getImageId().value(), event.getRetryCount(), e);
            
            // 예상치 못한 에러는 재시도 가능한 것으로 처리
            ThumbnailGenerationService.ThumbnailGenerationException retryableException = 
                    new ThumbnailGenerationService.ThumbnailGenerationException(
                        "Unexpected error: " + e.getMessage(), e, true);
            
            handleThumbnailGenerationFailure(event, retryableException);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 썸네일 생성 실패 처리
     */
    private void handleThumbnailGenerationFailure(ThumbnailGenerationRequestedEvent event, 
                                                 ThumbnailGenerationService.ThumbnailGenerationException exception) {
        
        boolean canRetry = exception.isRetryable() && retryStrategy.canRetry(event.getRetryCount());
        
        Optional<Image> imageOpt = imageRepository.findById(event.getImageId());
        if (imageOpt.isEmpty()) {
            logger.warn("Image not found during failure handling: {}", event.getImageId().value());
            return;
        }
        
        Image image = imageOpt.get();
        
        if (canRetry) {
            // 재시도 가능한 경우
            image.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.FAILED_RETRYABLE);
            imageRepository.save(image);
            
            // 지연 후 재시도 이벤트 발행
            long delayMillis = retryStrategy.calculateDelayMillis(event.getRetryCount());
            scheduleRetry(event, delayMillis);
            
            logger.info("Scheduled retry for image: {} after {}ms, retry count: {}", 
                       event.getImageId().value(), delayMillis, event.getRetryCount() + 1);
            
        } else {
            // 재시도 불가능한 경우
            image.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.FAILED_PERMANENT);
            imageRepository.save(image);
            
            logger.error("Thumbnail generation permanently failed for image: {}, retry count: {}", 
                        event.getImageId().value(), event.getRetryCount());
        }
        
        // 실패 이벤트 발행
        ThumbnailGenerationFailedEvent failedEvent = ThumbnailGenerationFailedEvent.of(
            event.getImageId(),
            exception,
            event.getRetryCount(),
            canRetry
        );
        eventPublisher.publishThumbnailGenerationFailed(failedEvent);
    }
    
    /**
     * 지연 후 재시도 이벤트 발행
     */
    private void scheduleRetry(ThumbnailGenerationRequestedEvent originalEvent, long delayMillis) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMillis);
                
                ThumbnailGenerationRequestedEvent retryEvent = originalEvent.withRetry();
                eventPublisher.publishThumbnailGenerationRequested(retryEvent);
                
                logger.info("Retry event published for image: {}, retry count: {}", 
                           originalEvent.getImageId().value(), retryEvent.getRetryCount());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Retry scheduling interrupted for image: {}", originalEvent.getImageId().value());
            }
        });
    }
    
    /**
     * 주기적으로 실패한 썸네일 생성 작업 재시도 (데드레터 처리)
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Transactional
    public void retryFailedThumbnailGeneration() {
        logger.debug("Checking for failed thumbnail generation tasks to retry...");
        
        // 구현 시 필요에 따라 데이터베이스에서 재시도 가능한 실패 상태의 이미지들을 조회하여 재시도
        // 현재는 이벤트 기반 재시도가 주된 메커니즘이므로 단순 로깅만 수행
    }
}