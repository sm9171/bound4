package com.bound4.project.adapter.in.web.dto;

import com.bound4.project.domain.Project;
import com.bound4.project.domain.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        ProjectStatus status,
        Long userId,
        String userEmail,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getUserId(),
                project.getUser().getEmail(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}