package com.bound4.image.adapter.out.event;

import com.bound4.image.application.port.out.EventPublisher;
import com.bound4.image.domain.event.ThumbnailGenerationCompletedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationFailedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring Events를 사용한 이벤트 발행자 구현
 */
@Component
public class SpringEventPublisher implements EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringEventPublisher.class);
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    @Override
    public void publishThumbnailGenerationRequested(ThumbnailGenerationRequestedEvent event) {
        logger.debug("Publishing thumbnail generation requested event: {}", event);
        applicationEventPublisher.publishEvent(event);
    }
    
    @Override
    public void publishThumbnailGenerationCompleted(ThumbnailGenerationCompletedEvent event) {
        logger.debug("Publishing thumbnail generation completed event: {}", event);
        applicationEventPublisher.publishEvent(event);
    }
    
    @Override
    public void publishThumbnailGenerationFailed(ThumbnailGenerationFailedEvent event) {
        logger.debug("Publishing thumbnail generation failed event: {}", event);
        applicationEventPublisher.publishEvent(event);
    }
}