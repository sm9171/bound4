package com.bound4.image.application.service;

import com.bound4.image.application.port.in.ImageCursorListQuery;
import com.bound4.image.application.port.in.ImageCursorListUseCase;
import com.bound4.image.application.port.in.ImageListQuery;
import com.bound4.image.application.port.in.ImageListUseCase;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("이미지 Cursor 페이징 서비스 테스트")
class ImageCursorListServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ImageListUseCase imageListUseCase;

    private ImageCursorListService imageCursorListService;

    private Image sampleImage1;
    private Image sampleImage2;
    private ImageCursorListQuery sampleQuery;

    @BeforeEach
    void setUp() {
        imageCursorListService = new ImageCursorListService(imageRepository, imageListUseCase);

        sampleImage1 = new Image(
                ProjectId.of(100L),
                "test1.jpg",
                FileHash.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                1024L,
                "image/jpeg",
                "original1.jpg");
        sampleImage1.setId(ImageId.of(1L));
        sampleImage1.setThumbnailKey("thumb1.jpg");
        sampleImage1.updateStatus(ImageStatus.READY);
        sampleImage1.updateMemo("Test image 1");
        sampleImage1.setVersion(1L);

        sampleImage2 = new Image(
                ProjectId.of(100L),
                "test2.jpg",
                FileHash.of("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"),
                2048L,
                "image/jpeg",
                "original2.jpg");
        sampleImage2.setId(ImageId.of(2L));
        sampleImage2.setThumbnailKey("thumb2.jpg");
        sampleImage2.updateStatus(ImageStatus.READY);
        sampleImage2.updateMemo("Test image 2");
        sampleImage2.setVersion(1L);

        sampleQuery = ImageCursorListQuery.builder()
                .projectId(100L)
                .size(20)
                .direction(SortDirection.DESC)
                .sortBy("createdAt")
                .build();
    }

    @Test
    @DisplayName("커서 기반 이미지 목록 조회 - 성공")
    void getImagesCursorBased_Success() {
        // Given
        List<Image> images = Arrays.asList(sampleImage1, sampleImage2);
        PageInfo pageInfo = PageInfo.of(true, false, 
                Cursor.of(2L, LocalDateTime.now()), null, 20, 2);
        ImageCursorListUseCase.CursorPageResult<Image> expectedResult = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);

        when(imageRepository.findImagesByCursor(any(ImageCursorListQuery.class)))
                .thenReturn(expectedResult);

        // When
        ImageCursorListUseCase.CursorPageResult<Image> result = 
                imageCursorListService.getImagesCursorBased(sampleQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(sampleImage1, sampleImage2);
        assertThat(result.getPageInfo().hasNext()).isTrue();
        assertThat(result.getPageInfo().hasPrevious()).isFalse();
        assertThat(result.getPageInfo().getSize()).isEqualTo(20);
        assertThat(result.getPageInfo().getActualSize()).isEqualTo(2);

        verify(imageRepository).findImagesByCursor(sampleQuery);
    }

    @Test
    @DisplayName("커서 기반 이미지 목록 조회 - 커서 포함")
    void getImagesCursorBased_WithCursor() {
        // Given
        Cursor cursor = Cursor.of(1L, LocalDateTime.now().minusHours(1));
        ImageCursorListQuery queryWithCursor = ImageCursorListQuery.builder()
                .projectId(100L)
                .cursor(cursor)
                .size(20)
                .direction(SortDirection.DESC)
                .sortBy("createdAt")
                .build();

        List<Image> images = Arrays.asList(sampleImage2);
        PageInfo pageInfo = PageInfo.of(false, true, null, 
                Cursor.of(1L, LocalDateTime.now()), 20, 1);
        ImageCursorListUseCase.CursorPageResult<Image> expectedResult = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);

        when(imageRepository.findImagesByCursor(any(ImageCursorListQuery.class)))
                .thenReturn(expectedResult);

        // When
        ImageCursorListUseCase.CursorPageResult<Image> result = 
                imageCursorListService.getImagesCursorBased(queryWithCursor);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).containsExactly(sampleImage2);
        assertThat(result.getPageInfo().hasNext()).isFalse();
        assertThat(result.getPageInfo().hasPrevious()).isTrue();

        verify(imageRepository).findImagesByCursor(queryWithCursor);
    }

    @Test
    @DisplayName("커서 기반 이미지 목록 조회 - 리포지토리 오류")
    void getImagesCursorBased_RepositoryError() {
        // Given
        when(imageRepository.findImagesByCursor(any(ImageCursorListQuery.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThatThrownBy(() -> imageCursorListService.getImagesCursorBased(sampleQuery))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to execute cursor-based pagination")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(imageRepository).findImagesByCursor(sampleQuery);
    }

    @Test
    @DisplayName("성능 비교 - 성공")
    void comparePerformance_Success() {
        // Given
        ImageListQuery offsetQuery = new ImageListQuery(
                100L, 1, 20, ImageStatus.READY, Arrays.asList("tag1"), "createdAt", "desc");

        // Mock offset pagination result
        Page offsetResult = new PageImpl(Arrays.asList("projection1", "projection2"));
        when(imageListUseCase.getImageList(any(ImageListQuery.class)))
                .thenReturn(offsetResult);

        // Mock cursor pagination result
        List<Image> images = Arrays.asList(sampleImage1, sampleImage2);
        PageInfo pageInfo = PageInfo.of(false, false, null, null, 20, 2);
        ImageCursorListUseCase.CursorPageResult<Image> cursorResult = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);
        when(imageRepository.findImagesByCursor(any(ImageCursorListQuery.class)))
                .thenReturn(cursorResult);

        // When
        ImageCursorListUseCase.PagingPerformanceMetrics metrics = 
                imageCursorListService.comparePerformance(offsetQuery, sampleQuery);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getOffsetResultCount()).isEqualTo(2);
        assertThat(metrics.getCursorResultCount()).isEqualTo(2);
        assertThat(metrics.getQueryComplexity()).contains("SORT_CREATEDAT");
        assertThat(metrics.getQueryComplexity()).contains("DIR_DESC");
        assertThat(metrics.getOffsetPagingDuration()).isNotNull();
        assertThat(metrics.getCursorPagingDuration()).isNotNull();

        verify(imageListUseCase).getImageList(offsetQuery);
        verify(imageRepository).findImagesByCursor(sampleQuery);
    }

    @Test
    @DisplayName("성능 비교 - offset 페이징 오류")
    void comparePerformance_OffsetError() {
        // Given
        ImageListQuery offsetQuery = new ImageListQuery(
                100L, 1, 20, ImageStatus.READY, Arrays.asList("tag1"), "createdAt", "desc");

        when(imageListUseCase.getImageList(any(ImageListQuery.class)))
                .thenThrow(new RuntimeException("Offset pagination error"));

        // When & Then
        assertThatThrownBy(() -> imageCursorListService.comparePerformance(offsetQuery, sampleQuery))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to execute offset pagination for comparison");

        verify(imageListUseCase).getImageList(offsetQuery);
        verify(imageRepository, never()).findImagesByCursor(any());
    }

    @Test
    @DisplayName("성능 비교 - cursor 페이징 오류")
    void comparePerformance_CursorError() {
        // Given
        ImageListQuery offsetQuery = new ImageListQuery(
                100L, 1, 20, ImageStatus.READY, Arrays.asList("tag1"), "createdAt", "desc");

        // Mock offset pagination success
        Page offsetResult = new PageImpl(Arrays.asList("projection1", "projection2"));
        when(imageListUseCase.getImageList(any(ImageListQuery.class)))
                .thenReturn(offsetResult);

        // Mock cursor pagination failure
        when(imageRepository.findImagesByCursor(any(ImageCursorListQuery.class)))
                .thenThrow(new RuntimeException("Cursor pagination error"));

        // When & Then
        assertThatThrownBy(() -> imageCursorListService.comparePerformance(offsetQuery, sampleQuery))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to execute cursor pagination for comparison");

        verify(imageListUseCase).getImageList(offsetQuery);
        verify(imageRepository).findImagesByCursor(sampleQuery);
    }

    @Test
    @DisplayName("성능 비교 - 복잡한 쿼리 조건 분석")
    void comparePerformance_ComplexQueryAnalysis() {
        // Given
        ImageListQuery offsetQuery = new ImageListQuery(
                100L, 15, 20, ImageStatus.READY, Arrays.asList("tag1", "tag2"), "updatedAt", "asc");

        ImageCursorListQuery cursorQuery = ImageCursorListQuery.builder()
                .projectId(100L)
                .cursor(Cursor.of(10L, LocalDateTime.now()))
                .size(20)
                .direction(SortDirection.ASC)
                .sortBy("updatedAt")
                .status(ImageStatus.READY)
                .tags(Arrays.asList("tag1", "tag2"))
                .build();

        // Mock results
        Page offsetResult = new PageImpl(Arrays.asList("projection1"));
        when(imageListUseCase.getImageList(any(ImageListQuery.class)))
                .thenReturn(offsetResult);

        List<Image> images = Arrays.asList(sampleImage1);
        PageInfo pageInfo = PageInfo.of(false, false, null, null, 20, 1);
        ImageCursorListUseCase.CursorPageResult<Image> cursorResult = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);
        when(imageRepository.findImagesByCursor(any(ImageCursorListQuery.class)))
                .thenReturn(cursorResult);

        // When
        ImageCursorListUseCase.PagingPerformanceMetrics metrics = 
                imageCursorListService.comparePerformance(offsetQuery, cursorQuery);

        // Then
        assertThat(metrics.getQueryComplexity()).contains("STATUS_FILTER");
        assertThat(metrics.getQueryComplexity()).contains("TAGS_FILTER");
        assertThat(metrics.getQueryComplexity()).contains("DEEP_OFFSET");
        assertThat(metrics.getQueryComplexity()).contains("CURSOR_NAVIGATION");
        assertThat(metrics.getQueryComplexity()).contains("SORT_UPDATEDAT");
        assertThat(metrics.getQueryComplexity()).contains("DIR_ASC");
    }
}