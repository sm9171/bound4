package com.bound4.project.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @NotBlank(message = "프로젝트 이름은 필수입니다")
        @Size(min = 1, max = 100, message = "프로젝트 이름은 1~100자 사이여야 합니다")
        String name,

        @Size(max = 500, message = "프로젝트 설명은 500자를 초과할 수 없습니다")
        String description
) {
}