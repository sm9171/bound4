package com.bound4.image.adapter.in.web;

import com.bound4.image.application.port.in.ImageCursorListUseCase;
import com.bound4.image.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageCursorController.class)
@DisplayName("이미지 Cursor 페이징 컨트롤러 통합 테스트")
class ImageCursorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImageCursorListUseCase imageCursorListUseCase;

    private Image sampleImage1;
    private Image sampleImage2;
    private Cursor sampleCursor;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
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

        sampleCursor = Cursor.of(1L, now.minusHours(1));
    }

    @Test
    @DisplayName("커서 기반 이미지 목록 조회 - 성공")
    void getImagesCursorBased_Success() throws Exception {
        // Given
        List<Image> images = Arrays.asList(sampleImage1, sampleImage2);
        PageInfo pageInfo = PageInfo.of(true, false, 
                Cursor.of(2L, LocalDateTime.now()), null, 20, 2);
        ImageCursorListUseCase.CursorPageResult<Image> result = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);

        when(imageCursorListUseCase.getImagesCursorBased(any())).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/v2/images/projects/100")
                .param("size", "20")
                .param("direction", "desc")
                .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.pageable.hasNext").value(true))
                .andExpect(jsonPath("$.data.pageable.hasPrevious").value(false))
                .andExpect(jsonPath("$.data.pageable.size").value(20))
                .andExpect(jsonPath("$.data.pageable.actualSize").value(2));
    }

    @Test
    @DisplayName("커서 기반 이미지 목록 조회 - 커서 포함")
    void getImagesCursorBased_WithCursor() throws Exception {
        // Given
        List<Image> images = Arrays.asList(sampleImage2);
        PageInfo pageInfo = PageInfo.of(false, true, null, 
                Cursor.of(1L, LocalDateTime.now()), 20, 1);
        ImageCursorListUseCase.CursorPageResult<Image> result = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);

        when(imageCursorListUseCase.getImagesCursorBased(any())).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/v2/images/projects/100")
                .param("cursor", sampleCursor.getEncodedValue())
                .param("size", "20")
                .param("direction", "desc")
                .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.pageable.hasNext").value(false))
                .andExpect(jsonPath("$.data.pageable.hasPrevious").value(true));
    }

    @Test
    @DisplayName("커서 기반 이미지 목록 조회 - 필터 조건 포함")
    void getImagesCursorBased_WithFilters() throws Exception {
        // Given
        List<Image> images = Arrays.asList(sampleImage1);
        PageInfo pageInfo = PageInfo.of(false, false, null, null, 20, 1);
        ImageCursorListUseCase.CursorPageResult<Image> result = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);

        when(imageCursorListUseCase.getImagesCursorBased(any())).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/v2/images/projects/100")
                .param("size", "20")
                .param("direction", "desc")
                .param("sortBy", "createdAt")
                .param("status", "READY")
                .param("tags", "tag1,tag2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("커서 기반 이미지 목록 조회 - 잘못된 커서 형식")
    void getImagesCursorBased_InvalidCursor() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v2/images/projects/100")
                .param("cursor", "invalid-cursor-format")
                .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid cursor format"));
    }

    @Test
    @DisplayName("커서 기반 이미지 목록 조회 - 잘못된 상태값")
    void getImagesCursorBased_InvalidStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v2/images/projects/100")
                .param("status", "INVALID_STATUS")
                .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid status: INVALID_STATUS"));
    }

    @Test
    @DisplayName("성능 비교 API - 성공")
    void comparePerformance_Success() throws Exception {
        // Given
        ImageCursorController.PerformanceComparisonRequest request = 
                new ImageCursorController.PerformanceComparisonRequest(
                        1, null, 20, "desc", "createdAt", ImageStatus.READY, Arrays.asList("tag1"));

        ImageCursorListUseCase.PagingPerformanceMetrics metrics = 
                new ImageCursorListUseCase.PagingPerformanceMetrics(
                        java.time.Duration.ofMillis(150),
                        java.time.Duration.ofMillis(50),
                        20, 20, "SORT_CREATEDAT,DIR_DESC");

        when(imageCursorListUseCase.comparePerformance(any(), any())).thenReturn(metrics);

        // When & Then
        mockMvc.perform(post("/api/v2/images/projects/100/performance-comparison")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offsetPagingDurationMs").value(150))
                .andExpect(jsonPath("$.data.cursorPagingDurationMs").value(50))
                .andExpect(jsonPath("$.data.offsetResultCount").value(20))
                .andExpect(jsonPath("$.data.cursorResultCount").value(20))
                .andExpect(jsonPath("$.data.performanceImprovementPercent").exists());
    }

    @Test
    @DisplayName("성능 비교 API - 서버 오류")
    void comparePerformance_ServerError() throws Exception {
        // Given
        ImageCursorController.PerformanceComparisonRequest request = 
                new ImageCursorController.PerformanceComparisonRequest(
                        1, null, 20, "desc", "createdAt", ImageStatus.READY, Arrays.asList("tag1"));

        when(imageCursorListUseCase.comparePerformance(any(), any()))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        mockMvc.perform(post("/api/v2/images/projects/100/performance-comparison")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Performance comparison failed: Database connection error"));
    }
}