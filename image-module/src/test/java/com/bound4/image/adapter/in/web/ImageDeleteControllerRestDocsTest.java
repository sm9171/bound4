package com.bound4.image.adapter.in.web;

import com.bound4.image.RestDocsConfiguration;
import com.bound4.image.adapter.in.web.exception.ImageAlreadyDeletedException;
import com.bound4.image.adapter.in.web.exception.ImageNotFoundException;
import com.bound4.image.application.port.in.ImageDataUseCase;
import com.bound4.image.application.port.in.ImageDeleteUseCase;
import com.bound4.image.application.port.in.ImageDetailUseCase;
import com.bound4.image.application.port.in.ImageUpdateUseCase;
import com.bound4.image.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageDetailController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, GlobalExceptionHandler.class})
class ImageDeleteControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageDetailUseCase imageDetailUseCase;

    @MockitoBean
    private ImageDataUseCase imageDataUseCase;

    @MockitoBean
    private ImageUpdateUseCase imageUpdateUseCase;

    @MockitoBean
    private ImageDeleteUseCase imageDeleteUseCase;

    @Test
    void deleteImage_Success() throws Exception {
        // Given
        Long imageId = 1L;
        Image mockDeletedImage = createDeletedMockImage(imageId);
        when(imageDeleteUseCase.deleteImage(any())).thenReturn(mockDeletedImage);

        // When & Then
        mockMvc.perform(delete("/images/{id}", imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(imageId))
                .andExpect(jsonPath("$.data.deletedAt").exists())
                .andExpect(jsonPath("$.message").value("이미지가 성공적으로 삭제되었습니다."))
                .andDo(document("delete-image",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("삭제할 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("삭제된 이미지 정보"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("삭제된 이미지 ID"),
                                fieldWithPath("data.deletedAt").type(JsonFieldType.STRING).description("삭제 시간 (ISO 8601 형식)"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 메시지")
                        )));
    }

    @Test
    void deleteImage_NotFound() throws Exception {
        // Given
        Long imageId = 999L;
        when(imageDeleteUseCase.deleteImage(any()))
                .thenThrow(new ImageNotFoundException(imageId));

        // When & Then
        mockMvc.perform(delete("/images/{id}", imageId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Image not found with id: " + imageId))
                .andDo(document("delete-image-not-found",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("존재하지 않는 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부 (false)"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 (실패 시 null)"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                        )));
    }

    @Test
    void deleteImage_AlreadyDeleted() throws Exception {
        // Given
        Long imageId = 1L;
        when(imageDeleteUseCase.deleteImage(any()))
                .thenThrow(new ImageAlreadyDeletedException(imageId));

        // When & Then
        mockMvc.perform(delete("/images/{id}", imageId))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Image is already deleted with id: " + imageId))
                .andDo(document("delete-image-already-deleted",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("이미 삭제된 이미지 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부 (false)"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 (실패 시 null)"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                        )));
    }

    private Image createDeletedMockImage(Long imageId) {
        ProjectId projectId = ProjectId.of(123L);
        FileHash fileHash = FileHash.of("a".repeat(64));
        String originalImageKey = "projects/123/images/original/" + fileHash.value() + "_original";

        Image image = new Image(projectId, "sample.jpg", fileHash, 1024000L, "image/jpeg", originalImageKey);
        image.setId(ImageId.of(imageId));
        image.setThumbnailKey("projects/123/images/thumbnail/" + fileHash.value() + "_thumbnail");
        image.updateTags(new HashMap<>());
        image.updateMemo("Test memo");
        
        // 삭제 처리
        image.markAsDeleted();

        return image;
    }
}