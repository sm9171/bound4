package com.bound4.image.adapter.in.web;

import com.bound4.image.RestDocsConfiguration;
import com.bound4.image.application.port.in.ImageCursorListUseCase;
import com.bound4.image.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageCursorController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, GlobalExceptionHandler.class})
class ImageCursorControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImageCursorListUseCase imageCursorListUseCase;

    @Test
    void getImagesCursorBased_Success() throws Exception {
        // Given
        Image sampleImage = new Image(
                ProjectId.of(100L),
                "test.jpg",
                FileHash.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                1024L,
                "image/jpeg",
                "original.jpg");
        sampleImage.setId(ImageId.of(1L));

        List<Image> images = Arrays.asList(sampleImage);
        PageInfo pageInfo = PageInfo.of(true, false, 
                Cursor.of(1L, LocalDateTime.now()), null, 20, 1);
        
        ImageCursorListUseCase.CursorPageResult<Image> result = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);

        when(imageCursorListUseCase.getImagesCursorBased(any())).thenReturn(result);

        mockMvc.perform(get("/api/v2/images/projects/{projectId}", 100L)
                        .param("size", "20")
                        .param("direction", "desc")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("cursor-pagination-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("projectId").description("프로젝트 ID")
                        ),
                        queryParameters(
                                parameterWithName("size").description("페이지 크기 (1-100, 기본값: 20)").optional(),
                                parameterWithName("direction").description("정렬 방향 (asc/desc, 기본값: desc)").optional(),
                                parameterWithName("sortBy").description("정렬 기준 (createdAt/updatedAt, 기본값: createdAt)").optional()
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("이미지 목록"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                                fieldWithPath("data.content[].projectId").type(JsonFieldType.NUMBER).description("프로젝트 ID"),
                                fieldWithPath("data.content[].filename").type(JsonFieldType.STRING).description("파일명"),
                                fieldWithPath("data.content[].fileSize").type(JsonFieldType.NUMBER).description("파일 크기"),
                                fieldWithPath("data.content[].mimeType").type(JsonFieldType.STRING).description("MIME 타입"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("이미지 상태"),
                                fieldWithPath("data.content[].tags").type(JsonFieldType.ARRAY).description("태그 목록").optional(),
                                fieldWithPath("data.content[].memo").type(JsonFieldType.STRING).description("메모").optional(),
                                fieldWithPath("data.content[].cursor").type(JsonFieldType.STRING).description("커서 토큰"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                                fieldWithPath("data.content[].updatedAt").type(JsonFieldType.STRING).description("수정 시간"),
                                fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이징 정보"),
                                fieldWithPath("data.pageable.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("data.pageable.hasPrevious").type(JsonFieldType.BOOLEAN).description("이전 페이지 존재 여부"),
                                fieldWithPath("data.pageable.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 커서").optional(),
                                fieldWithPath("data.pageable.previousCursor").type(JsonFieldType.STRING).description("이전 페이지 커서").optional(),
                                fieldWithPath("data.pageable.size").type(JsonFieldType.NUMBER).description("요청된 페이지 크기"),
                                fieldWithPath("data.pageable.actualSize").type(JsonFieldType.NUMBER).description("실제 반환된 항목 수"),
                                fieldWithPath("message").type(JsonFieldType.NULL).description("메시지 (성공 시 null)")
                        )
                ));
    }

    @Test
    void getImagesCursorBased_WithCursor() throws Exception {
        // Given
        Image sampleImage = new Image(
                ProjectId.of(100L),
                "test.jpg",
                FileHash.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                1024L,
                "image/jpeg",
                "original.jpg");
        sampleImage.setId(ImageId.of(2L));

        List<Image> images = Arrays.asList(sampleImage);
        PageInfo pageInfo = PageInfo.of(false, true, null, 
                Cursor.of(1L, LocalDateTime.now()), 20, 1);
        
        ImageCursorListUseCase.CursorPageResult<Image> result = 
                new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);

        when(imageCursorListUseCase.getImagesCursorBased(any())).thenReturn(result);

        String cursorToken = Cursor.of(1L, LocalDateTime.now()).getEncodedValue();

        mockMvc.perform(get("/api/v2/images/projects/{projectId}", 100L)
                        .param("cursor", cursorToken)
                        .param("size", "20")
                        .param("direction", "desc")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("cursor-pagination-with-cursor",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("projectId").description("프로젝트 ID")
                        ),
                        queryParameters(
                                parameterWithName("cursor").description("이전 페이지의 마지막 항목 커서"),
                                parameterWithName("size").description("페이지 크기").optional(),
                                parameterWithName("direction").description("정렬 방향").optional(),
                                parameterWithName("sortBy").description("정렬 기준").optional()
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("이미지 목록"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                                fieldWithPath("data.content[].projectId").type(JsonFieldType.NUMBER).description("프로젝트 ID"),
                                fieldWithPath("data.content[].filename").type(JsonFieldType.STRING).description("파일명"),
                                fieldWithPath("data.content[].fileSize").type(JsonFieldType.NUMBER).description("파일 크기"),
                                fieldWithPath("data.content[].mimeType").type(JsonFieldType.STRING).description("MIME 타입"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("이미지 상태"),
                                fieldWithPath("data.content[].tags").type(JsonFieldType.ARRAY).description("태그 목록").optional(),
                                fieldWithPath("data.content[].memo").type(JsonFieldType.STRING).description("메모").optional(),
                                fieldWithPath("data.content[].cursor").type(JsonFieldType.STRING).description("커서 토큰"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                                fieldWithPath("data.content[].updatedAt").type(JsonFieldType.STRING).description("수정 시간"),
                                fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이징 정보"),
                                fieldWithPath("data.pageable.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                                fieldWithPath("data.pageable.hasPrevious").type(JsonFieldType.BOOLEAN).description("이전 페이지 존재 여부"),
                                fieldWithPath("data.pageable.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 커서").optional(),
                                fieldWithPath("data.pageable.previousCursor").type(JsonFieldType.STRING).description("이전 페이지 커서").optional(),
                                fieldWithPath("data.pageable.size").type(JsonFieldType.NUMBER).description("요청된 페이지 크기"),
                                fieldWithPath("data.pageable.actualSize").type(JsonFieldType.NUMBER).description("실제 반환된 항목 수"),
                                fieldWithPath("message").type(JsonFieldType.NULL).description("메시지")
                        )
                ));
    }

    @Test
    void comparePerformance_Success() throws Exception {
        // Given
        ImageCursorController.PerformanceComparisonRequest request = 
                new ImageCursorController.PerformanceComparisonRequest(
                        1, null, 20, "desc", "createdAt", ImageStatus.READY, Arrays.asList("tag1"));

        ImageCursorListUseCase.PagingPerformanceMetrics metrics = 
                new ImageCursorListUseCase.PagingPerformanceMetrics(
                        Duration.ofMillis(150),
                        Duration.ofMillis(50),
                        20, 20, "SORT_CREATEDAT,DIR_DESC");

        when(imageCursorListUseCase.comparePerformance(any(), any())).thenReturn(metrics);

        mockMvc.perform(post("/api/v2/images/projects/{projectId}/performance-comparison", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("pagination-performance-comparison",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("projectId").description("프로젝트 ID")
                        ),
                        requestFields(
                                fieldWithPath("offsetPage").type(JsonFieldType.NUMBER).description("Offset 페이지 번호 (0부터 시작)"),
                                fieldWithPath("cursor").type(JsonFieldType.STRING).description("Cursor 토큰").optional(),
                                fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("direction").type(JsonFieldType.STRING).description("정렬 방향"),
                                fieldWithPath("sortBy").type(JsonFieldType.STRING).description("정렬 기준"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("이미지 상태 필터").optional(),
                                fieldWithPath("tags").type(JsonFieldType.ARRAY).description("태그 필터").optional()
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("성능 비교 결과"),
                                fieldWithPath("data.offsetPagingDurationMs").type(JsonFieldType.NUMBER).description("Offset 페이징 처리 시간 (밀리초)"),
                                fieldWithPath("data.cursorPagingDurationMs").type(JsonFieldType.NUMBER).description("Cursor 페이징 처리 시간 (밀리초)"),
                                fieldWithPath("data.offsetResultCount").type(JsonFieldType.NUMBER).description("Offset 페이징 결과 수"),
                                fieldWithPath("data.cursorResultCount").type(JsonFieldType.NUMBER).description("Cursor 페이징 결과 수"),
                                fieldWithPath("data.queryComplexity").type(JsonFieldType.STRING).description("쿼리 복잡도"),
                                fieldWithPath("data.performanceImprovementPercent").type(JsonFieldType.NUMBER).description("성능 개선률 (%)"),
                                fieldWithPath("data.performanceSummary").type(JsonFieldType.STRING).description("성능 개선 요약"),
                                fieldWithPath("message").type(JsonFieldType.NULL).description("메시지")
                        )
                ));
    }
}