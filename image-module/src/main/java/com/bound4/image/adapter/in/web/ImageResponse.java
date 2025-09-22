package com.bound4.image.adapter.in.web;

import com.bound4.image.domain.Image;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ImageResponse(
    Long id,
    Long projectId,
    String filename,
    Long fileSize,
    String mimeType,
    String status,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt
) {
    public static ImageResponse from(Image image) {
        return new ImageResponse(
            image.getId().value(),
            image.getProjectId().value(),
            image.getOriginalFilename(),
            image.getFileSize(),
            image.getMimeType(),
            image.getStatus().name(),
            image.getCreatedAt()
        );
    }
}