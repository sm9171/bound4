package com.bound4.image.application.port.out;

import com.bound4.image.domain.event.ThumbnailGenerationCompletedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationFailedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationRequestedEvent;

/**
 * 도메인 이벤트 발행을 위한 포트
 */
public interface EventPublisher {
    
    /**
     * 썸네일 생성 요청 이벤트 발행
     * @param event 썸네일 생성 요청 이벤트
     */
    void publishThumbnailGenerationRequested(ThumbnailGenerationRequestedEvent event);
    
    /**
     * 썸네일 생성 완료 이벤트 발행
     * @param event 썸네일 생성 완료 이벤트
     */
    void publishThumbnailGenerationCompleted(ThumbnailGenerationCompletedEvent event);
    
    /**
     * 썸네일 생성 실패 이벤트 발행
     * @param event 썸네일 생성 실패 이벤트
     */
    void publishThumbnailGenerationFailed(ThumbnailGenerationFailedEvent event);
}