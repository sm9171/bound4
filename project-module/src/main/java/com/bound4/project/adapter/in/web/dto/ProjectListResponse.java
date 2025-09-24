package com.bound4.project.adapter.in.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ProjectListResponse(
        List<ProjectResponse> projects,
        PageInfo pageInfo
) {
    public static ProjectListResponse from(Page<ProjectResponse> page) {
        return new ProjectListResponse(
                page.getContent(),
                new PageInfo(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isFirst(),
                        page.isLast(),
                        page.hasNext(),
                        page.hasPrevious()
                )
        );
    }

    public record PageInfo(
            int currentPage,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last,
            boolean hasNext,
            boolean hasPrevious
    ) {
    }
}