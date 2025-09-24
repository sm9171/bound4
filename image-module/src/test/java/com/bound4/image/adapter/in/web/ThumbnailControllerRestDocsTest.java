package com.bound4.image.adapter.in.web;

import com.bound4.image.RestDocsConfiguration;
import com.bound4.image.application.port.in.ThumbnailProcessingUseCase;
import com.bound4.image.domain.ThumbnailProcessingStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ThumbnailController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, GlobalExceptionHandler.class})
class ThumbnailControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ThumbnailProcessingUseCase thumbnailProcessingUseCase;

    @Test
    void requestThumbnailGeneration_Success() throws Exception {
        mockMvc.perform(post("/api/thumbnails/images/{imageId}/generate", 1L))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("thumbnail-generation-request-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("imageId").description("썸네일을 생성할 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                        )
                ));
    }

    @Test
    void requestThumbnailGeneration_ImageNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Image not found: 999"))
                .when(thumbnailProcessingUseCase).requestThumbnailGeneration(any());

        mockMvc.perform(post("/api/thumbnails/images/{imageId}/generate", 999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andDo(document("thumbnail-generation-request-not-found",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("imageId").description("존재하지 않는 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("오류 메시지")
                        )
                ));
    }

    @Test
    void getThumbnailStatus_Success() throws Exception {
        when(thumbnailProcessingUseCase.getThumbnailProcessingStatus(any()))
                .thenReturn(ThumbnailProcessingStatus.PROCESSING);

        mockMvc.perform(get("/api/thumbnails/images/{imageId}/status", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("thumbnail-status-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("imageId").description("상태를 조회할 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("썸네일 처리 상태 정보"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("처리 상태 (pending/processing/completed/failed_retryable/failed_permanent)"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("상태 설명"),
                                fieldWithPath("data.canRetry").type(JsonFieldType.BOOLEAN).description("재시도 가능 여부"),
                                fieldWithPath("data.completed").type(JsonFieldType.BOOLEAN).description("완료 여부"),
                                fieldWithPath("data.failed").type(JsonFieldType.BOOLEAN).description("실패 여부"),
                                fieldWithPath("data.inProgress").type(JsonFieldType.BOOLEAN).description("진행 중 여부"),
                                fieldWithPath("message").type(JsonFieldType.NULL).description("메시지 (성공 시 null)")
                        )
                ));
    }

    @Test
    void getThumbnailStatus_Completed() throws Exception {
        when(thumbnailProcessingUseCase.getThumbnailProcessingStatus(any()))
                .thenReturn(ThumbnailProcessingStatus.COMPLETED);

        mockMvc.perform(get("/api/thumbnails/images/{imageId}/status", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("thumbnail-status-completed",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("imageId").description("상태를 조회할 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("썸네일 처리 상태 정보"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("처리 상태"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("상태 설명"),
                                fieldWithPath("data.canRetry").type(JsonFieldType.BOOLEAN).description("재시도 가능 여부"),
                                fieldWithPath("data.completed").type(JsonFieldType.BOOLEAN).description("완료 여부"),
                                fieldWithPath("data.failed").type(JsonFieldType.BOOLEAN).description("실패 여부"),
                                fieldWithPath("data.inProgress").type(JsonFieldType.BOOLEAN).description("진행 중 여부"),
                                fieldWithPath("message").type(JsonFieldType.NULL).description("메시지")
                        )
                ));
    }

    @Test
    void retryThumbnailGeneration_Success() throws Exception {
        mockMvc.perform(post("/api/thumbnails/images/{imageId}/retry", 1L))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("thumbnail-generation-retry-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("imageId").description("재시도할 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                        )
                ));
    }

    @Test
    void retryThumbnailGeneration_CannotRetry() throws Exception {
        doThrow(new IllegalStateException("Cannot retry thumbnail generation"))
                .when(thumbnailProcessingUseCase).retryThumbnailGeneration(any());

        mockMvc.perform(post("/api/thumbnails/images/{imageId}/retry", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andDo(document("thumbnail-generation-retry-cannot-retry",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("imageId").description("재시도할 수 없는 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("오류 메시지")
                        )
                ));
    }
}