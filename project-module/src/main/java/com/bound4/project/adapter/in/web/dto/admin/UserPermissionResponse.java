package com.bound4.project.adapter.in.web.dto.admin;

import com.bound4.project.domain.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record UserPermissionResponse(
        UserInfo user,
        List<PermissionInfo> permissions,
        List<String> authorities
) {
    
    public record UserInfo(
            Long id,
            String email,
            Role role,
            SubscriptionPlan subscriptionPlan,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    user.getSubscriptionPlan(),
                    user.getCreatedAt()
            );
        }
    }
    
    public record PermissionInfo(
            Resource resource,
            Action action,
            boolean allowed,
            String source  // "ROLE_POLICY" | "PERMISSION" | "DEFAULT"
    ) {
    }
    
    public static UserPermissionResponse of(User user, List<PermissionInfo> permissions, List<String> authorities) {
        return new UserPermissionResponse(
                UserInfo.from(user),
                permissions,
                authorities
        );
    }
}