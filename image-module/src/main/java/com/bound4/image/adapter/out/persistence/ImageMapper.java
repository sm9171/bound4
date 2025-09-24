package com.bound4.image.adapter.out.persistence;

import com.bound4.image.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImageMapper {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ImageEntity toEntity(Image image) {
        ImageEntity entity = new ImageEntity();
        
        if (image.getId() != null) {
            entity.setId(image.getId().value());
        }
        entity.setProjectId(image.getProjectId().value());
        entity.setOriginalFilename(image.getOriginalFilename());
        entity.setFileHash(image.getFileHash().value());
        entity.setFileSize(image.getFileSize());
        entity.setMimeType(image.getMimeType());
        entity.setOriginalImageKey(image.getOriginalImageKey());
        entity.setThumbnailKey(image.getThumbnailKey());
        
        entity.setStatus(image.getStatus());
        entity.setTags(mapToJson(image.getTags()));
        entity.setMemo(image.getMemo());
        entity.setCreatedAt(image.getCreatedAt());
        entity.setUpdatedAt(image.getUpdatedAt());
        entity.setDeletedAt(image.getDeletedAt());
        
        return entity;
    }
    
    public Image toDomain(ImageEntity entity) {
        Image image = new Image(
            ProjectId.of(entity.getProjectId()),
            entity.getOriginalFilename(),
            FileHash.of(entity.getFileHash()),
            entity.getFileSize(),
            entity.getMimeType(),
            entity.getOriginalImageKey()
        );
        
        if (entity.getId() != null) {
            image.setId(ImageId.of(entity.getId()));
        }
        
        if (entity.getThumbnailKey() != null) {
            image.setThumbnailKey(entity.getThumbnailKey());
        }
        
        image.updateStatus(entity.getStatus());
        image.updateTags(jsonToMap(entity.getTags()));
        image.updateMemo(entity.getMemo());
        
        return image;
    }
    
    private String mapToJson(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert map to JSON", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to map", e);
        }
    }
}