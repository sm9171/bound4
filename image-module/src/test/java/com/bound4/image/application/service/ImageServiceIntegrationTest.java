package com.bound4.image.application.service;

import com.bound4.image.application.port.in.ImageDataQuery;
import com.bound4.image.application.port.in.ImageDeleteCommand;
import com.bound4.image.application.port.in.ImageDetailQuery;
import com.bound4.image.application.port.out.EventPublisher;
import com.bound4.image.application.port.out.FileStorageService;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.*;
import com.bound4.image.domain.event.ThumbnailGenerationCompletedEvent;
import com.bound4.image.domain.event.ThumbnailGenerationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("이미지 서비스 통합 테스트")
class ImageServiceIntegrationTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private FileStorageService fileStorageService;

    private ThumbnailProcessingService thumbnailProcessingService;
    private ImageDetailService imageDetailService;
    private ImageDataService imageDataService;

    private Image sampleImage;

    @BeforeEach
    void setUp() {
        thumbnailProcessingService = new ThumbnailProcessingService(imageRepository, eventPublisher);
        imageDetailService = new ImageDetailService(imageRepository);
        imageDataService = new ImageDataService(imageRepository, fileStorageService);

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
    @DisplayName("이미지 업로드 후 썸네일 처리 완전한 워크플로우")
    void completeImageProcessingWorkflow() {
        // Given: 이미지가 업로드된 상태 (PENDING)
        assertThat(sampleImage.getThumbnailProcessingStatus()).isEqualTo(ThumbnailProcessingStatus.PENDING);
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When 1: 썸네일 생성 요청
        thumbnailProcessingService.requestThumbnailGeneration(ImageId.of(1L));

        // Then 1: 상태가 PROCESSING으로 변경되고 이벤트 발행
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.PROCESSING));
        verify(eventPublisher).publishThumbnailGenerationRequested(any(ThumbnailGenerationRequestedEvent.class));

        // Given 2: 썸네일 생성 완료 시뮬레이션
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.PROCESSING);
        String thumbnailKey = "thumbnails/1_thumb.jpg";
        sampleImage.setThumbnailKey(thumbnailKey);

        // When 2: 썸네일 처리 상태 조회
        ThumbnailProcessingStatus status = thumbnailProcessingService.getThumbnailProcessingStatus(ImageId.of(1L));

        // Then 2: 완료 상태 확인
        assertThat(status).isEqualTo(ThumbnailProcessingStatus.COMPLETED);
        assertThat(sampleImage.getThumbnailKey()).isEqualTo(thumbnailKey);
    }

    @Test
    @DisplayName("이미지 상세 조회 - 썸네일 처리 상태 포함")
    void getImageDetail_WithThumbnailStatus() {
        // Given
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.PROCESSING);
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        ImageDetailQuery query = new ImageDetailQuery(1L);

        // When
        Image result = imageDetailService.getImageDetail(query);

        // Then
        assertThat(result.getId()).isEqualTo(ImageId.of(1L));
        assertThat(result.getThumbnailProcessingStatus()).isEqualTo(ThumbnailProcessingStatus.PROCESSING);
        assertThat(result.getThumbnailKey()).isNull(); // 아직 처리 중이므로 null
    }

    @Test
    @DisplayName("썸네일 이미지 데이터 조회 - 완료된 경우")
    void getThumbnailImageData_Completed() {
        // Given
        String thumbnailKey = "thumbnails/1_thumb.jpg";
        sampleImage.setThumbnailKey(thumbnailKey);
        byte[] thumbnailData = "thumbnail data".getBytes();

        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));
        when(fileStorageService.downloadFile(thumbnailKey)).thenReturn(thumbnailData);

        ImageDataQuery query = new ImageDataQuery(1L, ImageDataQuery.ImageDataType.THUMBNAIL);

        // When
        ImageDataService.ImageDataResponse response = imageDataService.getImageData(query);

        // Then
        assertThat(response.getImageData().getData()).isEqualTo(thumbnailData);
        assertThat(response.getMimeType()).isEqualTo("image/jpeg");
        assertThat(response.getFilename()).contains("thumb");
        verify(fileStorageService).downloadFile(thumbnailKey);
    }

    @Test
    @DisplayName("썸네일 처리 실패 후 재시도")
    void thumbnailProcessingFailureAndRetry() {
        // Given: 썸네일 처리 실패 상태
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.FAILED_RETRYABLE);
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When: 재시도 요청
        thumbnailProcessingService.retryThumbnailGeneration(ImageId.of(1L));

        // Then: 상태가 PROCESSING으로 변경되고 재시도 이벤트 발행
        verify(imageRepository).save(argThat(image -> 
                image.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.PROCESSING));
        verify(eventPublisher).publishThumbnailGenerationRequested(any(ThumbnailGenerationRequestedEvent.class));
    }

    @Test
    @DisplayName("영구 실패한 썸네일은 재시도 불가")
    void thumbnailPermanentFailure_CannotRetry() {
        // Given
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.FAILED_PERMANENT);
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));

        // When & Then
        assertThatThrows(IllegalStateException.class, () -> 
                thumbnailProcessingService.retryThumbnailGeneration(ImageId.of(1L)));

        verify(imageRepository, never()).save(any());
        verify(eventPublisher, never()).publishThumbnailGenerationRequested(any());
    }

    @Test
    @DisplayName("이미지 삭제 시 썸네일도 함께 삭제")
    void deleteImageWithThumbnail() {
        // Given
        sampleImage.setThumbnailKey("thumbnails/1_thumb.jpg");
        ImageDeleteService imageDeleteService = new ImageDeleteService(imageRepository, fileStorageService);
        
        when(imageRepository.findById(ImageId.of(1L))).thenReturn(Optional.of(sampleImage));
        when(imageRepository.save(any(Image.class))).thenReturn(sampleImage);

        ImageDeleteCommand command = new ImageDeleteCommand(1L);

        // When
        Image deletedImage = imageDeleteService.deleteImage(command);

        // Then
        assertThat(deletedImage.isDeleted()).isTrue();
        assertThat(deletedImage.getStatus()).isEqualTo(ImageStatus.DELETED);
        
        // 원본과 썸네일 모두 삭제 확인
        verify(fileStorageService).deleteFile("original.jpg");
        verify(fileStorageService).deleteFile("thumbnails/1_thumb.jpg");
        verify(imageRepository).save(argThat(Image::isDeleted));
    }

    @Test
    @DisplayName("비동기 이벤트 처리 시뮬레이션")
    void asyncEventProcessingSimulation() {
        when(imageRepository.save(any(Image.class))).thenReturn(sampleImage);

        // When: 이벤트 처리 시뮬레이션 (실제로는 AsyncThumbnailGenerationService에서 처리)
        // 1. 처리 시작
        sampleImage.updateThumbnailProcessingStatus(ThumbnailProcessingStatus.PROCESSING);
        imageRepository.save(sampleImage);

        // 2. 썸네일 생성 완료
        String thumbnailKey = "thumbnails/1_thumb.jpg";
        sampleImage.setThumbnailKey(thumbnailKey);

        // 3. 완료 이벤트 발행
        ThumbnailGenerationCompletedEvent completedEvent = ThumbnailGenerationCompletedEvent.of(
                ImageId.of(1L), thumbnailKey, 1500L);

        // Then: 상태 확인
        assertThat(sampleImage.getThumbnailProcessingStatus()).isEqualTo(ThumbnailProcessingStatus.COMPLETED);
        assertThat(sampleImage.getThumbnailKey()).isEqualTo(thumbnailKey);
        assertThat(completedEvent.getProcessingTimeMs()).isEqualTo(1500L);

        verify(imageRepository, atLeastOnce()).save(sampleImage);
    }

    private void assertThatThrows(Class<? extends Exception> expectedType, Runnable executable) {
        try {
            executable.run();
            throw new AssertionError("Expected " + expectedType.getSimpleName() + " to be thrown");
        } catch (Exception e) {
            if (!expectedType.isInstance(e)) {
                throw new AssertionError("Expected " + expectedType.getSimpleName() + " but got " + e.getClass().getSimpleName());
            }
        }
    }
}