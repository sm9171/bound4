package com.bound4.image.adapter.in.web;

import com.bound4.image.RestDocsConfiguration;
import com.bound4.image.application.port.in.ImageUpdateUseCase;
import com.bound4.image.application.port.in.ImageDetailUseCase;
import com.bound4.image.application.port.in.ImageDataUseCase;
import com.bound4.image.application.port.in.ImageDeleteUseCase;
import com.bound4.image.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import org.springframework.restdocs.payload.JsonFieldType;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageDetailController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, GlobalExceptionHandler.class})
class ImageUpdateControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImageUpdateUseCase imageUpdateUseCase;

    @MockitoBean
    private ImageDetailUseCase imageDetailUseCase;

    @MockitoBean
    private ImageDataUseCase imageDataUseCase;
    
    @MockitoBean
    private ImageDeleteUseCase imageDeleteUseCase;

    @Test
    void updateImage_Success() throws Exception {
        // Given
        Long imageId = 1L;
        List<String> tags = Arrays.asList("updated", "nature");
        String memo = "Updated description";
        ImageStatus status = ImageStatus.READY;
        Long version = 1L;

        Image mockImage = createMockImage(imageId, tags, memo, status, version + 1);
        when(imageUpdateUseCase.updateImage(any())).thenReturn(mockImage);

        ImageUpdateRequest request = new ImageUpdateRequest(tags, memo, status, version);

        // When & Then
        mockMvc.perform(patch("/images/{id}", imageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(imageId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("READY"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.memo").value(memo))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.version").value(version + 1))
                .andDo(document("image-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("이미지 ID")
                        ),
                        requestFields(
                                fieldWithPath("tags").description("태그 배열 (선택적)").optional(),
                                fieldWithPath("memo").description("메모 텍스트 (선택적)").optional(),
                                fieldWithPath("status").description("이미지 상태 (READY, PROCESSING, FAILED) (선택적)").optional(),
                                fieldWithPath("version").description("낙관적 락을 위한 버전 번호")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                                fieldWithPath("data.projectId").type(JsonFieldType.NUMBER).description("프로젝트 ID"),
                                fieldWithPath("data.filename").type(JsonFieldType.STRING).description("파일명"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("이미지 상태"),
                                fieldWithPath("data.tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                                fieldWithPath("data.memo").type(JsonFieldType.STRING).description("메모"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정일시"),
                                fieldWithPath("data.version").type(JsonFieldType.NUMBER).description("버전 번호"),
                                fieldWithPath("message").type(JsonFieldType.NULL).description("메시지 (성공 시 null)")
                        )));
    }

    @Test
    void updateImage_PartialUpdate() throws Exception {
        // Given
        Long imageId = 1L;
        String memo = "Only memo updated";
        Long version = 2L;

        Image mockImage = createMockImage(imageId, Arrays.asList(), memo, null, version + 1);
        when(imageUpdateUseCase.updateImage(any())).thenReturn(mockImage);

        ImageUpdateRequest request = new ImageUpdateRequest(null, memo, null, version);

        // When & Then
        mockMvc.perform(patch("/images/{id}", imageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.memo").value(memo))
;
    }

    private Image createMockImage(Long imageId, List<String> tags, String memo, ImageStatus status, Long version) {
        ProjectId projectId = ProjectId.of(123L);
        String filename = "sample.jpg";
        FileHash fileHash = FileHash.of("a".repeat(64));
        long fileSize = 1024L;
        String mimeType = "image/jpeg";
        String originalImageKey = "projects/123/images/original/" + fileHash.value() + "_original";

        Image image = new Image(projectId, filename, fileHash, fileSize, mimeType, originalImageKey);
        image.setId(ImageId.of(imageId));
        image.setVersion(version);
        
        if (tags != null) {
            Map<String, Object> tagMap = tags.stream()
                    .collect(java.util.stream.Collectors.toMap(tag -> tag, tag -> true));
            image.updateTags(tagMap);
        }
        
        if (memo != null) {
            image.updateMemo(memo);
        }
        
        if (status != null) {
            image.updateStatus(status);
        }

        return image;
    }
}