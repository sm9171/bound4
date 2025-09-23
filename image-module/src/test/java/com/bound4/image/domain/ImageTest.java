package com.bound4.image.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTest {
    
    @Test
    void createImage_Success() {
        // Given
        ProjectId projectId = ProjectId.of(123L);
        String filename = "test.jpg";
        FileHash fileHash = FileHash.of("a".repeat(64));
        long fileSize = 1024L;
        String mimeType = "image/jpeg";
        String originalImageKey = "projects/123/images/original/" + fileHash.value() + "_original";
        
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        
        // When
        Image image = new Image(projectId, filename, fileHash, fileSize, mimeType, originalImageKey);
        
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
        
        // Then
        assertThat(image.getProjectId()).isEqualTo(projectId);
        assertThat(image.getOriginalFilename()).isEqualTo(filename);
        assertThat(image.getFileHash()).isEqualTo(fileHash);
        assertThat(image.getFileSize()).isEqualTo(fileSize);
        assertThat(image.getMimeType()).isEqualTo(mimeType);
        assertThat(image.getOriginalImageKey()).isEqualTo(originalImageKey);
        assertThat(image.getStatus()).isEqualTo(ImageStatus.READY);
        assertThat(image.getCreatedAt()).isBetween(beforeCreation, afterCreation);
        assertThat(image.getUpdatedAt()).isBetween(beforeCreation, afterCreation);
        assertThat(image.getThumbnailKey()).isNull();
        assertThat(image.getTags()).isNull();
        assertThat(image.getMemo()).isNull();
        assertThat(image.getDeletedAt()).isNull();
        assertThat(image.isDeleted()).isFalse();
    }
    
    @Test
    void setThumbnail_Success() {
        // Given
        Image image = createTestImage();
        String thumbnailKey = "projects/1/images/thumbnail/abc123_thumbnail";
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        
        // When
        image.setThumbnailKey(thumbnailKey);
        
        LocalDateTime afterUpdate = LocalDateTime.now().plusSeconds(1);
        
        // Then
        assertThat(image.getThumbnailKey()).isEqualTo(thumbnailKey);
        assertThat(image.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
    }
    
    @Test
    void updateStatus_Success() {
        // Given
        Image image = createTestImage();
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        
        // When
        image.updateStatus(ImageStatus.PROCESSING);
        
        LocalDateTime afterUpdate = LocalDateTime.now().plusSeconds(1);
        
        // Then
        assertThat(image.getStatus()).isEqualTo(ImageStatus.PROCESSING);
        assertThat(image.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
    }
    
    @Test
    void updateTags_Success() {
        // Given
        Image image = createTestImage();
        Map<String, Object> tags = new HashMap<>();
        tags.put("category", "nature");
        tags.put("rating", 5);
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        
        // When
        image.updateTags(tags);
        
        LocalDateTime afterUpdate = LocalDateTime.now().plusSeconds(1);
        
        // Then
        assertThat(image.getTags()).isEqualTo(tags);
        assertThat(image.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
    }
    
    @Test
    void updateMemo_Success() {
        // Given
        Image image = createTestImage();
        String memo = "This is a test image";
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        
        // When
        image.updateMemo(memo);
        
        LocalDateTime afterUpdate = LocalDateTime.now().plusSeconds(1);
        
        // Then
        assertThat(image.getMemo()).isEqualTo(memo);
        assertThat(image.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
    }
    
    @Test
    void markAsDeleted_Success() {
        // Given
        Image image = createTestImage();
        LocalDateTime beforeDeletion = LocalDateTime.now().minusSeconds(1);
        
        // When
        image.markAsDeleted();
        
        LocalDateTime afterDeletion = LocalDateTime.now().plusSeconds(1);
        
        // Then
        assertThat(image.getStatus()).isEqualTo(ImageStatus.DELETED);
        assertThat(image.getDeletedAt()).isBetween(beforeDeletion, afterDeletion);
        assertThat(image.getUpdatedAt()).isBetween(beforeDeletion, afterDeletion);
        assertThat(image.isDeleted()).isTrue();
    }
    
    @Test
    void isDeleted_WhenDeletedAtIsNull_ReturnsFalse() {
        // Given
        Image image = createTestImage();
        
        // When & Then
        assertThat(image.isDeleted()).isFalse();
    }
    
    @Test
    void isDeleted_WhenDeletedAtIsNotNull_ReturnsTrue() {
        // Given
        Image image = createTestImage();
        
        // When
        image.markAsDeleted();
        
        // Then
        assertThat(image.isDeleted()).isTrue();
    }
    
    @Test
    void setId_Success() {
        // Given
        Image image = createTestImage();
        ImageId imageId = ImageId.of(456L);
        
        // When
        image.setId(imageId);
        
        // Then
        assertThat(image.getId()).isEqualTo(imageId);
    }
    
    private Image createTestImage() {
        ProjectId projectId = ProjectId.of(123L);
        String filename = "test.jpg";
        FileHash fileHash = FileHash.of("a".repeat(64));
        long fileSize = 1024L;
        String mimeType = "image/jpeg";
        String originalImageKey = "projects/123/images/original/" + fileHash.value() + "_original";
        
        return new Image(projectId, filename, fileHash, fileSize, mimeType, originalImageKey);
    }
}