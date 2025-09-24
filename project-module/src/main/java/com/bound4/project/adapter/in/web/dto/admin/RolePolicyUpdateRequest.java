package com.bound4.project.adapter.in.web.dto.admin;

import com.bound4.project.domain.Action;
import com.bound4.project.domain.Resource;
import com.bound4.project.domain.Role;
import com.bound4.project.domain.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RolePolicyUpdateRequest(
        @NotNull(message = "역할은 필수입니다")
        Role role,
        
        @NotNull(message = "리소스는 필수입니다")
        Resource resource,
        
        @NotNull(message = "액션은 필수입니다")
        Action action,
        
        @NotNull(message = "허용 여부는 필수입니다")
        Boolean allowed,
        
        @NotNull(message = "구독 플랜은 필수입니다")
        SubscriptionPlan subscriptionPlan,
        
        @Size(max = 200, message = "이유는 200자를 초과할 수 없습니다")
        String reason
) {
}