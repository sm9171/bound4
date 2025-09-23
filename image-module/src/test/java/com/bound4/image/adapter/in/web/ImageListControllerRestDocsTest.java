package com.bound4.image.adapter.in.web;

import com.bound4.image.RestDocsConfiguration;
import com.bound4.image.adapter.out.persistence.ImageQueryRepository;
import com.bound4.image.application.port.in.ImageListUseCase;
import com.bound4.image.application.port.in.ImageUploadUseCase;
import com.bound4.image.domain.ImageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageController.class)
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
class ImageListControllerRestDocsTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ImageListUseCase imageListUseCase;
    
    @MockitoBean
    private ImageUploadUseCase imageUploadUseCase;
    
    @Test
    void getImages_Success() throws Exception {
        // Given
        List<ImageQueryRepository.ImageListProjection> projections = Arrays.asList(
            createProjection(1L, "sample1.jpg", ImageStatus.READY, "nature,landscape"),
            createProjection(2L, "sample2.png", ImageStatus.PROCESSING, "portrait,indoor")
        );
        Page<ImageQueryRepository.ImageListProjection> page = new PageImpl<>(
            projections, PageRequest.of(0, 20), 150L
        );
        
        when(imageListUseCase.getImageList(any())).thenReturn(page);
        
        // When & Then
        mockMvc.perform(get("/projects/{projectId}/images", 123)
                .param("page", "0")
                .param("size", "20")
                .param("status", "READY")
                .param("tags", "nature,landscape")
                .param("sort", "createdAt")
                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].filename").value("sample1.jpg"))
                .andExpect(jsonPath("$.data.content[0].status").value("READY"))
                .andExpect(jsonPath("$.data.pageable.page").value(0))
                .andExpect(jsonPath("$.data.pageable.size").value(20))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(150))
                .andExpect(jsonPath("$.data.pageable.totalPages").value(8))
                .andDo(document("get-images-list",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("projectId").description("프로젝트 ID")
                    ),
                    queryParameters(
                        parameterWithName("page").description("페이지 번호 (기본값: 0)").optional(),
                        parameterWithName("size").description("페이지 크기 (기본값: 20, 최대: 100)").optional(),
                        parameterWithName("status").description("이미지 상태 필터 (READY, PROCESSING, FAILED)").optional(),
                        parameterWithName("tags").description("태그 필터 (콤마로 구분된 문자열)").optional(),
                        parameterWithName("sort").description("정렬 기준 (createdAt, updatedAt, filename)").optional(),
                        parameterWithName("direction").description("정렬 방향 (asc, desc)").optional()
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                        fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("이미지 목록"),
                        fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                        fieldWithPath("data.content[].projectId").type(JsonFieldType.NUMBER).description("프로젝트 ID"),
                        fieldWithPath("data.content[].filename").type(JsonFieldType.STRING).description("원본 파일명"),
                        fieldWithPath("data.content[].fileSize").type(JsonFieldType.NUMBER).description("파일 크기 (bytes)"),
                        fieldWithPath("data.content[].mimeType").type(JsonFieldType.STRING).description("MIME 타입"),
                        fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("이미지 상태"),
                        fieldWithPath("data.content[].tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                        fieldWithPath("data.content[].memo").type(JsonFieldType.STRING).description("메모").optional(),
                        fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성 시간 (ISO 8601 형식)"),
                        fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이징 정보"),
                        fieldWithPath("data.pageable.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                        fieldWithPath("data.pageable.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                        fieldWithPath("data.pageable.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                        fieldWithPath("data.pageable.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
                    )
                ));
    }
    
    @Test
    void getImages_DefaultParameters() throws Exception {
        // Given
        List<ImageQueryRepository.ImageListProjection> projections = Arrays.asList(
            createProjection(1L, "sample1.jpg", ImageStatus.READY, null)
        );
        Page<ImageQueryRepository.ImageListProjection> page = new PageImpl<>(
            projections, PageRequest.of(0, 20), 1L
        );
        
        when(imageListUseCase.getImageList(any())).thenReturn(page);
        
        // When & Then
        mockMvc.perform(get("/projects/{projectId}/images", 123))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageable.page").value(0))
                .andExpect(jsonPath("$.data.pageable.size").value(20))
                .andDo(document("get-images-list-default",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("projectId").description("프로젝트 ID")
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                        fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("이미지 목록"),
                        fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                        fieldWithPath("data.content[].projectId").type(JsonFieldType.NUMBER).description("프로젝트 ID"),
                        fieldWithPath("data.content[].filename").type(JsonFieldType.STRING).description("원본 파일명"),
                        fieldWithPath("data.content[].fileSize").type(JsonFieldType.NUMBER).description("파일 크기 (bytes)"),
                        fieldWithPath("data.content[].mimeType").type(JsonFieldType.STRING).description("MIME 타입"),
                        fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("이미지 상태"),
                        fieldWithPath("data.content[].tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                        fieldWithPath("data.content[].memo").type(JsonFieldType.STRING).description("메모").optional(),
                        fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성 시간 (ISO 8601 형식)"),
                        fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이징 정보"),
                        fieldWithPath("data.pageable.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                        fieldWithPath("data.pageable.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                        fieldWithPath("data.pageable.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                        fieldWithPath("data.pageable.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
                    )
                ));
    }
    
    @Test
    void getImages_EmptyResult() throws Exception {
        // Given
        Page<ImageQueryRepository.ImageListProjection> emptyPage = new PageImpl<>(
            Arrays.asList(), PageRequest.of(0, 20), 0L
        );
        
        when(imageListUseCase.getImageList(any())).thenReturn(emptyPage);
        
        // When & Then
        mockMvc.perform(get("/projects/{projectId}/images", 123))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.pageable.totalElements").value(0))
                .andExpect(jsonPath("$.data.pageable.totalPages").value(0))
                .andDo(document("get-images-list-empty",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("projectId").description("프로젝트 ID")
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                        fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("빈 이미지 목록"),
                        fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이징 정보"),
                        fieldWithPath("data.pageable.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                        fieldWithPath("data.pageable.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                        fieldWithPath("data.pageable.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수 (0)"),
                        fieldWithPath("data.pageable.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
                    )
                ));
    }
    
    private ImageQueryRepository.ImageListProjection createProjection(Long id, String filename, 
                                                                     ImageStatus status, String tags) {
        return new ImageQueryRepository.ImageListProjection(
            id, 123L, filename, 1024000L, "image/jpeg", 
            status, tags, "Beautiful sunset", 
            LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        );
    }
}