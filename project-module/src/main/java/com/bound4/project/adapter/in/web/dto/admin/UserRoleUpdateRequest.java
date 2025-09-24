package com.bound4.project.adapter.in.web.dto.admin;

import com.bound4.project.domain.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRoleUpdateRequest(
        @NotNull(message = "새 역할은 필수입니다")
        Role newRole,
        
        @Size(max = 200, message = "변경 사유는 200자를 초과할 수 없습니다")
        String reason
) {
}