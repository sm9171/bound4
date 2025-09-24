package com.bound4.image.application.service;

import com.bound4.image.adapter.in.web.exception.DuplicateImageException;
import com.bound4.image.application.port.in.ThumbnailProcessingUseCase;
import com.bound4.image.application.port.in.UploadImageCommand;
import com.bound4.image.application.port.out.FileStorageService;
import com.bound4.image.application.port.out.HashService;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("이미지 업로드 서비스 통합 테스트")
class ImageUploadServiceIntegrationTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ThumbnailProcessingUseCase thumbnailProcessingUseCase;

    @Mock
    private HashService hashService;

    @Mock
    private FileStorageService fileStorageService;

    private ImageUploadService imageUploadService;

    private UploadImageCommand sampleCommand;
    private FileHash sampleFileHash;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService(
                imageRepository, thumbnailProcessingUseCase, hashService, fileStorageService);

        byte[] imageData = "test image data".getBytes();
        sampleCommand = new UploadImageCommand(
                ProjectId.of(100L),
                "test.jpg",
                "image/jpeg",
                imageData
        );

        sampleFileHash = FileHash.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("이미지 업로드 성공 - 비동기 썸네일 생성 요청")
    void uploadImages_Success_WithAsyncThumbnailGeneration() {
        // Given
        when(hashService.calculateHash(any())).thenReturn(sampleFileHash);
        when(imageRepository.findByHash(sampleFileHash)).thenReturn(Optional.empty());

        Image savedImage = new Image(
                sampleCommand.projectId(),
                sampleCommand.originalFilename(),
                sampleFileHash,
                sampleCommand.data().length,
                sampleCommand.mimeType(),
                "original_key"
        );
        savedImage.setId(ImageId.of(1L));
        
        when(imageRepository.save(any(Image.class))).thenReturn(savedImage);

        // When
        List<Image> result = imageUploadService.uploadImages(List.of(sampleCommand));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(ImageId.of(1L));
        assertThat(result.get(0).getThumbnailProcessingStatus()).isEqualTo(ThumbnailProcessingStatus.PENDING);

        // 파일 저장 검증
        verify(fileStorageService).uploadFile(
                "projects/100/images/original/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855_original",
                sampleCommand.data(),
                "image/jpeg"
        );

        // 이미지 저장 검증
        verify(imageRepository).save(any(Image.class));

        // 비동기 썸네일 생성 요청 검증
        verify(thumbnailProcessingUseCase).requestThumbnailGeneration(ImageId.of(1L));
    }

    @Test
    @DisplayName("이미지 업로드 실패 - 중복 이미지")
    void uploadImages_Failure_DuplicateImage() {
        // Given
        when(hashService.calculateHash(any())).thenReturn(sampleFileHash);
        
        Image existingImage = new Image(
                sampleCommand.projectId(),
                "existing.jpg",
                sampleFileHash,
                1024L,
                "image/jpeg",
                "existing_key"
        );
        when(imageRepository.findByHash(sampleFileHash)).thenReturn(Optional.of(existingImage));

        // When & Then
        assertThatThrownBy(() -> imageUploadService.uploadImages(List.of(sampleCommand)))
                .isInstanceOf(DuplicateImageException.class)
                .hasMessageContaining("already exists");

        verify(fileStorageService, never()).uploadFile(any(), any(), any());
        verify(imageRepository, never()).save(any());
        verify(thumbnailProcessingUseCase, never()).requestThumbnailGeneration(any());
    }

    @Test
    @DisplayName("썸네일 생성 요청 실패 시 예외 발생")
    void uploadImages_ThumbnailRequestFailed() {
        // Given
        when(hashService.calculateHash(any())).thenReturn(sampleFileHash);
        when(imageRepository.findByHash(sampleFileHash)).thenReturn(Optional.empty());

        Image savedImage = new Image(
                sampleCommand.projectId(),
                sampleCommand.originalFilename(),
                sampleFileHash,
                sampleCommand.data().length,
                sampleCommand.mimeType(),
                "original_key"
        );
        savedImage.setId(ImageId.of(1L));
        
        when(imageRepository.save(any(Image.class))).thenReturn(savedImage);
        doThrow(new RuntimeException("Thumbnail service unavailable"))
                .when(thumbnailProcessingUseCase).requestThumbnailGeneration(any());

        // When & Then
        assertThatThrownBy(() -> imageUploadService.uploadImages(List.of(sampleCommand)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to request thumbnail generation");

        verify(fileStorageService).uploadFile(any(), any(), any());
        verify(imageRepository).save(any());
        verify(thumbnailProcessingUseCase).requestThumbnailGeneration(ImageId.of(1L));
    }

    @Test
    @DisplayName("다중 이미지 업로드 성공")
    void uploadImages_MultipleImages_Success() {
        // Given
        UploadImageCommand command1 = new UploadImageCommand(
                ProjectId.of(100L), "test1.jpg", "image/jpeg", "data1".getBytes());
        UploadImageCommand command2 = new UploadImageCommand(
                ProjectId.of(100L), "test2.jpg", "image/jpeg", "data2".getBytes());

        FileHash hash1 = FileHash.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        FileHash hash2 = FileHash.of("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae");

        when(hashService.calculateHash(command1.data())).thenReturn(hash1);
        when(hashService.calculateHash(command2.data())).thenReturn(hash2);
        when(imageRepository.findByHash(any())).thenReturn(Optional.empty());

        Image savedImage1 = new Image(command1.projectId(), command1.originalFilename(), 
                hash1, command1.data().length, command1.mimeType(), "key1");
        savedImage1.setId(ImageId.of(1L));
        
        Image savedImage2 = new Image(command2.projectId(), command2.originalFilename(), 
                hash2, command2.data().length, command2.mimeType(), "key2");
        savedImage2.setId(ImageId.of(2L));

        when(imageRepository.save(any(Image.class)))
                .thenReturn(savedImage1)
                .thenReturn(savedImage2);

        // When
        List<Image> result = imageUploadService.uploadImages(List.of(command1, command2));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(ImageId.of(1L));
        assertThat(result.get(1).getId()).isEqualTo(ImageId.of(2L));

        verify(fileStorageService, times(2)).uploadFile(any(), any(), any());
        verify(imageRepository, times(2)).save(any());
        verify(thumbnailProcessingUseCase).requestThumbnailGeneration(ImageId.of(1L));
        verify(thumbnailProcessingUseCase).requestThumbnailGeneration(ImageId.of(2L));
    }

    @Test
    @DisplayName("Storage Key 생성 검증")
    void uploadImages_StorageKeyGeneration() {
        // Given
        when(hashService.calculateHash(any())).thenReturn(sampleFileHash);
        when(imageRepository.findByHash(sampleFileHash)).thenReturn(Optional.empty());

        Image savedImage = new Image(
                sampleCommand.projectId(),
                sampleCommand.originalFilename(),
                sampleFileHash,
                sampleCommand.data().length,
                sampleCommand.mimeType(),
                "original_key"
        );
        savedImage.setId(ImageId.of(1L));
        
        when(imageRepository.save(any(Image.class))).thenReturn(savedImage);

        // When
        imageUploadService.uploadImages(List.of(sampleCommand));

        // Then
        String expectedKey = "projects/100/images/original/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855_original";
        verify(fileStorageService).uploadFile(
                expectedKey,
                sampleCommand.data(),
                "image/jpeg"
        );
    }
}