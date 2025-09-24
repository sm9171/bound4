package com.bound4.project.adapter.in.web.dto.admin;

import com.bound4.project.domain.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record RolePolicyResponse(
        Long id,
        Role role,
        Resource resource,
        Action action,
        boolean allowed,
        SubscriptionPlan subscriptionPlan,
        String reason,
        boolean systemPolicy,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static RolePolicyResponse from(RolePolicy rolePolicy) {
        return new RolePolicyResponse(
                rolePolicy.getId(),
                rolePolicy.getRole(),
                rolePolicy.getResource(),
                rolePolicy.getAction(),
                rolePolicy.isAllowed(),
                rolePolicy.getSubscriptionPlan(),
                rolePolicy.getReason(),
                rolePolicy.isSystemPolicy(),
                rolePolicy.getCreatedAt(),
                rolePolicy.getUpdatedAt()
        );
    }
}