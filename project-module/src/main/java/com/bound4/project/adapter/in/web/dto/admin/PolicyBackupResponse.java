package com.bound4.project.adapter.in.web.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record PolicyBackupResponse(
        String backupId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        Long adminUserId,
        String adminEmail,
        int policyCount,
        List<RolePolicyResponse> policies
) {
}