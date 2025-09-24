package com.bound4.image.application.service;

import com.bound4.image.application.port.out.EventPublisher;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.application.port.out.ThumbnailGenerationService;
import com.bound4.image.domain.*;
import com.bound4.image.domain.event.ThumbnailGenerationCompletedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("비동기 썸네일 생성 서비스 테스트")
class AsyncThumbnailGenerationServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ThumbnailGenerationService thumbnailGenerationService;

    @Mock
    private EventPublisher eventPublisher;

    private AsyncThumbnailGenerationService asyncThumbnailGenerationService;

    private Image sampleImage;
    private ThumbnailGenerationRequestedEvent sampleEvent;

    @BeforeEach
    void setUp() {
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
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.PROCESSING);

        sampleEvent = new ThumbnailGenerationRequestedEvent(
                ImageId.of(1L),
                ProjectId.of(100L),
                "original.jpg",
                "test.jpg",
                "image/jpeg");
    }

    @Test
    @DisplayName("썸네일 생성 성공")
    void handleThumbnailGenerationRequested_Success() throws Exception {
        // Given
        String thumbnailKey = "thumbnails/1_thumb.jpg";
        
        when(imageRepository.findById(any(ImageId.class))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/jpeg")).thenReturn(true);
        when(thumbnailGenerationService.generateThumbnail(any(), any(), any())).thenReturn(thumbnailKey);

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(sampleEvent);
        result.get(); // 비동기 처리 완료 대기

        // Then
        verify(thumbnailGenerationService).generateThumbnail(
                ImageId.of(1L), "original.jpg", "image/jpeg");
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailKey().equals(thumbnailKey) &&
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.COMPLETED));
        verify(eventPublisher).publishThumbnailGenerationCompleted(any(ThumbnailGenerationCompletedEvent.class));
    }

    @Test
    @DisplayName("지원하지 않는 MIME 타입으로 영구 실패")
    void handleThumbnailGenerationRequested_UnsupportedMimeType() throws Exception {
        // Given
        ThumbnailGenerationRequestedEvent unsupportedEvent = new ThumbnailGenerationRequestedEvent(
                ImageId.of(1L), ProjectId.of(100L), "original.bmp", "test.bmp", "image/bmp");
        
        when(imageRepository.findById(any(ImageId.class))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/bmp")).thenReturn(false);

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(unsupportedEvent);
        result.get();

        // Then
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.FAILED_PERMANENT));
        verify(eventPublisher).publishThumbnailGenerationFailed(
                argThat(event -> !event.canRetry()));
        verify(thumbnailGenerationService, never()).generateThumbnail(any(), any(), any());
    }

    @Test
    @DisplayName("재시도 가능한 실패")
    void handleThumbnailGenerationRequested_RetryableFailure() throws Exception {
        // Given
        when(imageRepository.findById(any(ImageId.class))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/jpeg")).thenReturn(true);
        when(thumbnailGenerationService.generateThumbnail(any(), any(), any()))
                .thenThrow(new ThumbnailGenerationService.ThumbnailGenerationException(
                        "Temporary failure", true));

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(sampleEvent);
        result.get();

        // Then
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.FAILED_RETRYABLE));
        verify(eventPublisher).publishThumbnailGenerationFailed(
                argThat(event -> event.canRetry()));
    }

    @Test
    @DisplayName("재시도 불가능한 실패")
    void handleThumbnailGenerationRequested_PermanentFailure() throws Exception {
        // Given
        when(imageRepository.findById(any(ImageId.class))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/jpeg")).thenReturn(true);
        when(thumbnailGenerationService.generateThumbnail(any(), any(), any()))
                .thenThrow(new ThumbnailGenerationService.ThumbnailGenerationException(
                        "Permanent failure", false));

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(sampleEvent);
        result.get();

        // Then
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.FAILED_PERMANENT));
        verify(eventPublisher).publishThumbnailGenerationFailed(
                argThat(event -> !event.canRetry()));
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시 영구 실패")
    void handleThumbnailGenerationRequested_MaxRetriesExceeded() throws Exception {
        // Given
        ThumbnailGenerationRequestedEvent maxRetryEvent = new ThumbnailGenerationRequestedEvent(
                ImageId.of(1L), ProjectId.of(100L), "original.jpg", "test.jpg", "image/jpeg",
                LocalDateTime.now(), 5); // 최대 재시도 횟수 초과
        
        when(imageRepository.findById(any(ImageId.class))).thenReturn(Optional.of(sampleImage));
        when(thumbnailGenerationService.isSupported("image/jpeg")).thenReturn(true);
        when(thumbnailGenerationService.generateThumbnail(any(), any(), any()))
                .thenThrow(new ThumbnailGenerationService.ThumbnailGenerationException(
                        "Retryable failure", true));

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(maxRetryEvent);
        result.get();

        // Then
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.FAILED_PERMANENT));
        verify(eventPublisher).publishThumbnailGenerationFailed(
                argThat(event -> !event.canRetry()));
    }

    @Test
    @DisplayName("이미지가 존재하지 않는 경우")
    void handleThumbnailGenerationRequested_ImageNotFound() throws Exception {
        // Given
        when(imageRepository.findById(any(ImageId.class))).thenReturn(Optional.empty());

        // When
        CompletableFuture<Void> result = asyncThumbnailGenerationService
                .handleThumbnailGenerationRequested(sampleEvent);
        result.get();

        // Then
        verify(thumbnailGenerationService, never()).generateThumbnail(any(), any(), any());
        verify(eventPublisher, never()).publishThumbnailGenerationCompleted(any());
        verify(eventPublisher, never()).publishThumbnailGenerationFailed(any());
    }
}