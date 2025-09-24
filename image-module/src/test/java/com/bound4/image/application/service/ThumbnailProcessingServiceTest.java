package com.bound4.image.application.service;

import com.bound4.image.application.port.out.EventPublisher;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.*;
import com.bound4.image.domain.event.ThumbnailGenerationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("썸네일 처리 서비스 테스트")
class ThumbnailProcessingServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private EventPublisher eventPublisher;

    private ThumbnailProcessingService thumbnailProcessingService;

    private Image sampleImage;

    @BeforeEach
    void setUp() {
        thumbnailProcessingService = new ThumbnailProcessingService(imageRepository, eventPublisher);

        sampleImage = new Image(
                ProjectId.of(100L),
                "test.jpg",
                FileHash.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                1024L,
                "image/jpeg",
                "original.jpg");
        sampleImage.setId(ImageId.of(1L));
    }

    @Test
    @DisplayName("썸네일 생성 요청 - 성공")
    void requestThumbnailGeneration_Success() {
        // Given
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When
        thumbnailProcessingService.requestThumbnailGeneration(ImageId.of(1L));

        // Then
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.PROCESSING));
        verify(eventPublisher).publishThumbnailGenerationRequested(any(ThumbnailGenerationRequestedEvent.class));
    }

    @Test
    @DisplayName("썸네일 생성 요청 - 이미 완료된 경우 스킵")
    void requestThumbnailGeneration_AlreadyCompleted() {
        // Given
        sampleImage.setThumbnailKey("existing_thumbnail.jpg"); // COMPLETED 상태로 변경됨
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When
        thumbnailProcessingService.requestThumbnailGeneration(ImageId.of(1L));

        // Then
        verify(imageRepository, never()).save(any());
        verify(eventPublisher, never()).publishThumbnailGenerationRequested(any());
    }

    @Test
    @DisplayName("썸네일 생성 요청 - 처리 중인 경우 스킵")
    void requestThumbnailGeneration_AlreadyProcessing() {
        // Given
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.PROCESSING);
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When
        thumbnailProcessingService.requestThumbnailGeneration(ImageId.of(1L));

        // Then
        verify(imageRepository, never()).save(any());
        verify(eventPublisher, never()).publishThumbnailGenerationRequested(any());
    }

    @Test
    @DisplayName("썸네일 생성 요청 - 이미지 없음")
    void requestThumbnailGeneration_ImageNotFound() {
        // Given
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> thumbnailProcessingService.requestThumbnailGeneration(ImageId.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image not found: 1");
    }

    @Test
    @DisplayName("썸네일 처리 상태 조회 - 성공")
    void getThumbnailProcessingStatus_Success() {
        // Given
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.PROCESSING);
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When
        ThumbnailProcessingStatus status = thumbnailProcessingService.getThumbnailProcessingStatus(ImageId.of(1L));

        // Then
        assertThat(status).isEqualTo(ThumbnailProcessingStatus.PROCESSING);
    }

    @Test
    @DisplayName("썸네일 처리 상태 조회 - 이미지 없음")
    void getThumbnailProcessingStatus_ImageNotFound() {
        // Given
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> thumbnailProcessingService.getThumbnailProcessingStatus(ImageId.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image not found: 1");
    }

    @Test
    @DisplayName("썸네일 생성 재시도 - 성공")
    void retryThumbnailGeneration_Success() {
        // Given
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.FAILED_RETRYABLE);
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When
        thumbnailProcessingService.retryThumbnailGeneration(ImageId.of(1L));

        // Then
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.PROCESSING));
        verify(eventPublisher).publishThumbnailGenerationRequested(any(ThumbnailGenerationRequestedEvent.class));
    }

    @Test
    @DisplayName("썸네일 생성 재시도 - 재시도 불가능한 상태")
    void retryThumbnailGeneration_CannotRetry() {
        // Given
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.FAILED_PERMANENT);
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When & Then
        assertThatThrownBy(() -> thumbnailProcessingService.retryThumbnailGeneration(ImageId.of(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot retry thumbnail generation");
    }

    @Test
    @DisplayName("썸네일 생성 재시도 - 이미지 없음")
    void retryThumbnailGeneration_ImageNotFound() {
        // Given
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> thumbnailProcessingService.retryThumbnailGeneration(ImageId.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image not found: 1");
    }
}