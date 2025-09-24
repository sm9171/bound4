package com.bound4.image.application.service;

import com.bound4.image.application.port.out.EventPublisher;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.application.port.out.ThumbnailGenerationService;
import com.bound4.image.domain.*;
import com.bound4.image.domain.event.ThumbnailGenerationCompletedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationFailedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("이벤트 기반 썸네일 처리 테스트")
class EventDrivenThumbnailProcessingTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ThumbnailGenerationService thumbnailGenerationService;

    @Mock
    private EventPublisher eventPublisher;

    private ThumbnailProcessingService thumbnailProcessingService;
    private AsyncThumbnailGenerationService asyncThumbnailGenerationService;

    private Image sampleImage;
    private ThumbnailGenerationRequestedEvent sampleEvent;

    @BeforeEach
    void setUp() {
        thumbnailProcessingService = new ThumbnailProcessingService(imageRepository, eventPublisher);
        asyncThumbnailGenerationService = new AsyncThumbnailGenerationService(
                imageRepository, thumbnailGenerationService, eventPublisher);

        sampleImage = new Image(
                ProjectId.of(100L),
                "test.jpg",
                FileHash.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                1024L,
                "image/jpeg",
                "original.jpg");
        sampleImage.setId(ImageId.of(1L));

        sampleEvent = new ThumbnailGenerationRequestedEvent(
                ImageId.of(1L),
                ProjectId.of(100L),
                "original.jpg",
                "test.jpg",
                "image/jpeg");
    }

    @Test
    @DisplayName("완전한 이벤트 기반 썸네일 처리 워크플로우")
    void completeEventDrivenWorkflow() throws Exception {
        // Given
        String thumbnailKey = "thumbnails/1_thumb.jpg";
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/jpeg")).thenReturn(true);
        when(thumbnailGenerationService.generateThumbnail(any(), any(), any())).thenReturn(thumbnailKey);

        // When 1: 썸네일 생성 요청
        thumbnailProcessingService.requestThumbnailGeneration(ImageId.of(1L));

        // Then 1: 요청 이벤트 발행 검증
        ArgumentCaptor<ThumbnailGenerationRequestedEvent> requestEventCaptor = 
                ArgumentCaptor.forClass(ThumbnailGenerationRequestedEvent.class);
        verify(eventPublisher).publishThumbnailGenerationRequested(requestEventCaptor.capture());
        
        ThumbnailGenerationRequestedEvent capturedEvent = requestEventCaptor.getValue();
        assertThat(capturedEvent.getImageId()).isEqualTo(ImageId.of(1L));
        assertThat(capturedEvent.getProjectId()).isEqualTo(ProjectId.of(100L));
        assertThat(capturedEvent.getRetryCount()).isZero();

        // When 2: 비동기 이벤트 처리
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(capturedEvent);
        result.get(); // 비동기 처리 완료 대기

        // Then 2: 썸네일 생성 및 완료 이벤트 발행 검증
        verify(thumbnailGenerationService).generateThumbnail(
                ImageId.of(1L), "original.jpg", "image/jpeg");
        
        verify(imageRepository, atLeast(1)).save(any(Image.class));

        ArgumentCaptor<ThumbnailGenerationCompletedEvent> completedEventCaptor = 
                ArgumentCaptor.forClass(ThumbnailGenerationCompletedEvent.class);
        verify(eventPublisher).publishThumbnailGenerationCompleted(completedEventCaptor.capture());
        
        ThumbnailGenerationCompletedEvent completedEvent = completedEventCaptor.getValue();
        assertThat(completedEvent.getImageId()).isEqualTo(ImageId.of(1L));
        assertThat(completedEvent.getThumbnailKey()).isEqualTo(thumbnailKey);
    }

    @Test
    @DisplayName("재시도 가능한 실패 후 지수 백오프 재시도")
    void retryableFailureWithExponentialBackoff() throws Exception {
        // Given
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/jpeg")).thenReturn(true);
        when(thumbnailGenerationService.generateThumbnail(any(), any(), any()))
                .thenThrow(new ThumbnailGenerationService.ThumbnailGenerationException(
                        "Temporary failure", true));

        // When: 첫 번째 실패
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(sampleEvent);
        result.get();

        // Then: 실패 이벤트 발행 및 재시도 스케줄링 검증
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.FAILED_RETRYABLE));

        ArgumentCaptor<ThumbnailGenerationFailedEvent> failedEventCaptor = 
                ArgumentCaptor.forClass(ThumbnailGenerationFailedEvent.class);
        verify(eventPublisher).publishThumbnailGenerationFailed(failedEventCaptor.capture());
        
        ThumbnailGenerationFailedEvent failedEvent = failedEventCaptor.getValue();
        assertThat(failedEvent.getImageId()).isEqualTo(ImageId.of(1L));
        assertThat(failedEvent.canRetry()).isTrue();
        assertThat(failedEvent.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시 영구 실패")
    void maxRetriesExceededPermanentFailure() throws Exception {
        // Given: 최대 재시도 횟수를 초과한 이벤트
        ThumbnailGenerationRequestedEvent maxRetryEvent = new ThumbnailGenerationRequestedEvent(
                ImageId.of(1L), ProjectId.of(100L), "original.jpg", "test.jpg", "image/jpeg",
                LocalDateTime.now(), 5); // 최대 재시도 횟수 초과

        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/jpeg")).thenReturn(true);
        when(thumbnailGenerationService.generateThumbnail(any(), any(), any()))
                .thenThrow(new ThumbnailGenerationService.ThumbnailGenerationException(
                        "Retryable failure", true));

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(maxRetryEvent);
        result.get();

        // Then: 영구 실패로 처리
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.FAILED_PERMANENT));

        ArgumentCaptor<ThumbnailGenerationFailedEvent> failedEventCaptor = 
                ArgumentCaptor.forClass(ThumbnailGenerationFailedEvent.class);
        verify(eventPublisher).publishThumbnailGenerationFailed(failedEventCaptor.capture());
        
        ThumbnailGenerationFailedEvent failedEvent = failedEventCaptor.getValue();
        assertThat(failedEvent.canRetry()).isFalse();
        assertThat(failedEvent.getRetryCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("지원하지 않는 MIME 타입으로 영구 실패")
    void unsupportedMimeTypePermanentFailure() throws Exception {
        // Given
        ThumbnailGenerationRequestedEvent unsupportedEvent = new ThumbnailGenerationRequestedEvent(
                ImageId.of(1L), ProjectId.of(100L), "original.bmp", "test.bmp", "image/bmp");

        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/bmp")).thenReturn(false);

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(unsupportedEvent);
        result.get();

        // Then: 지원하지 않는 포맷으로 영구 실패
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.FAILED_PERMANENT));

        verify(thumbnailGenerationService, never()).generateThumbnail(any(), any(), any());

        ArgumentCaptor<ThumbnailGenerationFailedEvent> failedEventCaptor = 
                ArgumentCaptor.forClass(ThumbnailGenerationFailedEvent.class);
        verify(eventPublisher).publishThumbnailGenerationFailed(failedEventCaptor.capture());
        
        ThumbnailGenerationFailedEvent failedEvent = failedEventCaptor.getValue();
        assertThat(failedEvent.canRetry()).isFalse();
        assertThat(failedEvent.getErrorMessage()).contains("Unsupported MIME type");
    }

    @Test
    @DisplayName("이벤트 처리 중 이미지 삭제된 경우")
    void imageDeletedDuringProcessing() throws Exception {
        // Given: 이미지가 삭제된 상태
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.empty());

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(sampleEvent);
        result.get();

        // Then: 아무 처리 없이 종료
        verify(thumbnailGenerationService, never()).generateThumbnail(any(), any(), any());
        verify(eventPublisher, never()).publishThumbnailGenerationCompleted(any());
        verify(eventPublisher, never()).publishThumbnailGenerationFailed(any());
    }

    @Test
    @DisplayName("성능 메트릭 수집 검증")
    void performanceMetricsCollection() throws Exception {
        // Given
        String thumbnailKey = "thumbnails/1_thumb.jpg";
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/jpeg")).thenReturn(true);
        when(thumbnailGenerationService.generateThumbnail(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(100); // 처리 시간 시뮬레이션
                    return thumbnailKey;
                });

        // When
        long startTime = System.currentTimeMillis();
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(sampleEvent);
        result.get();
        long endTime = System.currentTimeMillis();

        // Then: 처리 시간이 적절히 측정되었는지 확인
        ArgumentCaptor<ThumbnailGenerationCompletedEvent> completedEventCaptor = 
                ArgumentCaptor.forClass(ThumbnailGenerationCompletedEvent.class);
        verify(eventPublisher).publishThumbnailGenerationCompleted(completedEventCaptor.capture());
        
        ThumbnailGenerationCompletedEvent completedEvent = completedEventCaptor.getValue();
        long processingTime = completedEvent.getProcessingTimeMs();
        
        assertThat(processingTime).isGreaterThanOrEqualTo(100); // 최소 처리 시간
        assertThat(processingTime).isLessThan(endTime - startTime + 1000); // 합리적인 범위
    }
}