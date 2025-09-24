package com.bound4.image.adapter.in.web;

import com.bound4.image.RestDocsConfiguration;
import com.bound4.image.application.port.in.ImageListUseCase;
import com.bound4.image.application.port.in.ImageUploadUseCase;
import com.bound4.image.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageController.class)
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
class ImageControllerRestDocsTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageUploadUseCase imageUploadUseCase;
    
    @MockitoBean
    private ImageListUseCase imageListUseCase;

    @Test
    void uploadImages_Success() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile(
            "files", "sample1.jpg", "image/jpeg", "sample image content 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
            "files", "sample2.png", "image/png", "sample image content 2".getBytes());
        
        Image mockImage1 = createMockImage(1L, "sample1.jpg", "image/jpeg", 1024000L);
        Image mockImage2 = createMockImage(2L, "sample2.png", "image/png", 2048000L);
        
        when(imageUploadUseCase.uploadImages(any()))
            .thenReturn(Arrays.asList(mockImage1, mockImage2));
        
        // When & Then
        mockMvc.perform(multipart("/projects/{projectId}/images", 123)
                .file(file1)
                .file(file2))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].filename").value("sample1.jpg"))
                .andDo(document("upload-images-success",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("projectId").description("프로젝트 ID")
                    ),
                    requestParts(
                        partWithName("files").description("업로드할 이미지 파일들 (멀티파트)")
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.ARRAY).description("업로드된 이미지 정보 배열"),
                        fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                        fieldWithPath("data[].projectId").type(JsonFieldType.NUMBER).description("프로젝트 ID"),
                        fieldWithPath("data[].filename").type(JsonFieldType.STRING).description("원본 파일명"),
                        fieldWithPath("data[].fileSize").type(JsonFieldType.NUMBER).description("파일 크기 (bytes)"),
                        fieldWithPath("data[].mimeType").type(JsonFieldType.STRING).description("MIME 타입"),
                        fieldWithPath("data[].status").type(JsonFieldType.STRING).description("이미지 상태 (READY, PROCESSING, FAILED, DELETED)"),
                        fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성 시간 (ISO 8601 형식)"),
                        fieldWithPath("message").type(JsonFieldType.NULL).description("에러 메시지 (성공 시 null)")
                    )
                ));
    }
    
    @Test
    void uploadImages_NoFiles() throws Exception {
        mockMvc.perform(multipart("/projects/{projectId}/images", 123))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No files provided"))
                .andDo(document("upload-images-no-files",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("projectId").description("프로젝트 ID")
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 (실패 시 null)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                    )
                ));
    }
    
    @Test
    void uploadImages_EmptyFiles() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "files", "empty.jpg", "image/jpeg", new byte[0]);
        
        mockMvc.perform(multipart("/projects/{projectId}/images", 123)
                .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No valid files provided"))
                .andDo(document("upload-images-empty-files",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("projectId").description("프로젝트 ID")
                    ),
                    requestParts(
                        partWithName("files").description("빈 파일 또는 유효하지 않은 파일")
                    ),
                    responseFields(
                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                        fieldWithPath("data").type(JsonFieldType.NULL).description("데이터 (실패 시 null)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                    )
                ));
    }
    
    private Image createMockImage(Long id, String filename, String mimeType, Long fileSize) {
        ProjectId projectId = ProjectId.of(123L);
        FileHash fileHash = FileHash.of("a".repeat(64));
        String originalImageKey = "projects/123/images/original/" + fileHash.value() + "_original";
        
        Image image = new Image(projectId, filename, fileHash, fileSize, mimeType, originalImageKey);
        image.setId(ImageId.of(id));
        image.setThumbnailKey("projects/123/images/thumbnail/" + fileHash.value() + "_thumbnail");
        
        return image;
    }
}