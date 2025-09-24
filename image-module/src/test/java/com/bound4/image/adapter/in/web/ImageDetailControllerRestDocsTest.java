package com.bound4.image.adapter.in.web;

import com.bound4.image.RestDocsConfiguration;
import com.bound4.image.adapter.in.web.exception.ImageNotFoundException;
import com.bound4.image.application.port.in.ImageDataUseCase;
import com.bound4.image.application.port.in.ImageDetailUseCase;
import com.bound4.image.application.port.in.ImageUpdateUseCase;
import com.bound4.image.application.port.in.ImageDeleteUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageDetailController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, GlobalExceptionHandler.class})
class ImageDetailControllerRestDocsTest {
    
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
    void getImageDetail_Success() throws Exception {
        // Given
        Image mockImage = createMockImage();
        when(imageDetailUseCase.getImageDetail(any())).thenReturn(mockImage);
        
        // When & Then
        mockMvc.perform(get("/images/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.filename").value("sample.jpg"))
                .andExpect(jsonPath("$.data.originalImageUrl").value("/images/1/original"))
                .andExpect(jsonPath("$.data.thumbnailUrl").value("/images/1/thumbnail"))
                .andDo(document("get-image-detail",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("id").description("이미지 ID")
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).description("이미지 상세 정보"),
                        fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                        fieldWithPath("data.projectId").type(JsonFieldType.NUMBER).description("프로젝트 ID"),
                        fieldWithPath("data.filename").type(JsonFieldType.STRING).description("원본 파일명"),
                        fieldWithPath("data.fileSize").type(JsonFieldType.NUMBER).description("파일 크기 (bytes)"),
                        fieldWithPath("data.mimeType").type(JsonFieldType.STRING).description("MIME 타입"),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("이미지 상태"),
                        fieldWithPath("data.tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                        fieldWithPath("data.memo").type(JsonFieldType.STRING).description("메모"),
                        fieldWithPath("data.originalImageUrl").type(JsonFieldType.STRING).description("원본 이미지 URL"),
                        fieldWithPath("data.thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL"),
                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시간 (ISO 8601 형식)"),
                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시간 (ISO 8601 형식)"),
                        fieldWithPath("message").type(JsonFieldType.NULL).description("메시지 (성공 시 null)")
                    )
                ));
    }
    
    @Test
    void getImageDetail_NotFound() throws Exception {
        // Given
        when(imageDetailUseCase.getImageDetail(any()))
                .thenThrow(new ImageNotFoundException(999L));
        
        // When & Then
        mockMvc.perform(get("/images/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Image not found with id: 999"))
                .andDo(document("get-image-detail-not-found",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("id").description("존재하지 않는 이미지 ID")
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부 (false)"),
                        fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 (실패 시 null)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                    )
                ));
    }
    
    @Test 
    void getOriginalImage_Success() throws Exception {
        // Given
        byte[] imageData = "original image data".getBytes();
        ImageData mockImageData = ImageData.of(imageData);
        ImageDataUseCase.ImageDataResponse response = new ImageDataUseCase.ImageDataResponse(
            mockImageData, "image/jpeg", "sample.jpg"
        );
        
        when(imageDataUseCase.getImageData(any())).thenReturn(response);
        
        // When & Then
        mockMvc.perform(get("/images/{id}/original", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(imageData))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"sample.jpg\""))
                .andDo(document("get-original-image",
                    preprocessRequest(prettyPrint()),
                    pathParameters(
                        parameterWithName("id").description("이미지 ID")
                    )
                ));
    }
    
    @Test
    void getThumbnailImage_Success() throws Exception {
        // Given
        byte[] thumbnailData = "thumbnail image data".getBytes();
        ImageData mockImageData = ImageData.of(thumbnailData);
        ImageDataUseCase.ImageDataResponse response = new ImageDataUseCase.ImageDataResponse(
            mockImageData, "image/jpeg", "thumb_sample.jpg"
        );
        
        when(imageDataUseCase.getImageData(any())).thenReturn(response);
        
        // When & Then
        mockMvc.perform(get("/images/{id}/thumbnail", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(thumbnailData))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"thumb_sample.jpg\""))
                .andDo(document("get-thumbnail-image",
                    preprocessRequest(prettyPrint()),
                    pathParameters(
                        parameterWithName("id").description("이미지 ID")
                    )
                ));
    }
    
    
    private Image createMockImage() {
        ProjectId projectId = ProjectId.of(123L);
        FileHash fileHash = FileHash.of("a".repeat(64));
        String originalImageKey = "projects/123/images/original/" + fileHash.value() + "_original";
        
        Image image = new Image(projectId, "sample.jpg", fileHash, 1024000L, "image/jpeg", originalImageKey);
        image.setId(ImageId.of(1L));
        image.setThumbnailKey("projects/123/images/thumbnail/" + fileHash.value() + "_thumbnail");
        image.updateTags(new HashMap<>());
        image.updateMemo("Test memo");
        
        return image;
    }
}