package com.bound4.project.config.security;

import com.bound4.project.application.service.PermissionService;
import com.bound4.project.config.security.JwtTokenProvider.JwtAuthenticationDetails;
import com.bound4.project.domain.SubscriptionPlan;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

public class CustomSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final PermissionService permissionService;
    private Object filterObject;
    private Object returnObject;

    public CustomSecurityExpressionRoot(Authentication authentication, PermissionService permissionService) {
        super(authentication);
        this.permissionService = permissionService;
    }

    /**
     * 프로젝트에 대한 읽기 권한 확인
     * 사용법: @PreAuthorize("hasProjectPermission('READ')")
     */
    public boolean hasProjectPermission(String action) {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        
        SubscriptionPlan subscriptionPlan = getCurrentSubscriptionPlan();
        return permissionService.checkProjectAccess(userId, action, subscriptionPlan);
    }

    /**
     * 특정 프로젝트에 대한 권한 확인
     * 사용법: @PreAuthorize("hasProjectPermission(#projectId, 'UPDATE')")
     */
    public boolean hasProjectPermission(Long projectId, String action) {
        Long userId = getCurrentUserId();
        if (userId == null || projectId == null) return false;
        
        return permissionService.checkProjectAccess(userId, projectId, action);
    }

    /**
     * 리소스에 대한 일반적인 권한 확인
     * 사용법: @PreAuthorize("hasResourcePermission('USER', 'CREATE')")
     */
    public boolean hasResourcePermission(String resource, String action) {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        
        return permissionService.hasPermission(userId, resource, action);
    }

    /**
     * 관리자 권한 확인
     * 사용법: @PreAuthorize("isAdmin()")
     */
    public boolean isAdmin() {
        return hasRole("A");
    }

    /**
     * 프리미엄 사용자 권한 확인
     * 사용법: @PreAuthorize("isPremiumUser()")
     */
    public boolean isPremiumUser() {
        return hasRole("B");
    }

    /**
     * 구독 플랜 확인
     * 사용법: @PreAuthorize("hasSubscriptionPlan('PRO')")
     */
    public boolean hasSubscriptionPlan(String planName) {
        SubscriptionPlan currentPlan = getCurrentSubscriptionPlan();
        if (currentPlan == null) return false;
        
        try {
            SubscriptionPlan requiredPlan = SubscriptionPlan.valueOf(planName);
            return currentPlan == requiredPlan || currentPlan.isUpgradeFrom(requiredPlan);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 프로젝트 소유자인지 확인
     * 사용법: @PreAuthorize("isProjectOwner(#projectId)")
     */
    public boolean isProjectOwner(Long projectId) {
        Long userId = getCurrentUserId();
        if (userId == null || projectId == null) return false;
        
        // 프로젝트 소유자 확인 로직은 PermissionService에서 처리
        return permissionService.checkProjectAccess(userId, projectId, "READ");
    }

    /**
     * 사용자 본인인지 확인
     * 사용법: @PreAuthorize("isSelf(#userId)")
     */
    public boolean isSelf(Long userId) {
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * 사용자 본인이거나 관리자인지 확인
     * 사용법: @PreAuthorize("isSelfOrAdmin(#userId)")
     */
    public boolean isSelfOrAdmin(Long userId) {
        return isSelf(userId) || isAdmin();
    }

    /**
     * 구독 제한 검증
     * 사용법: @PreAuthorize("canCreateProject()")
     */
    public boolean canCreateProject() {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        
        try {
            permissionService.validateProjectCreationLimit(userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 특정 플랜 이상 권한 확인
     * 사용법: @PreAuthorize("requiresSubscriptionPlan('PRO')")
     */
    public boolean requiresSubscriptionPlan(String planName) {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        
        try {
            SubscriptionPlan requiredPlan = SubscriptionPlan.valueOf(planName);
            permissionService.validateSubscriptionLimit(userId, requiredPlan);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth == null) return null;
        
        Object details = auth.getDetails();
        if (details instanceof JwtAuthenticationDetails jwtAuthenticationDetails) {
            return jwtAuthenticationDetails.getUserId();
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetailsService.CustomUserPrincipal customUserPrincipal) {
            return customUserPrincipal.getId();
        }
        
        return null;
    }

    private SubscriptionPlan getCurrentSubscriptionPlan() {
        Authentication auth = getAuthentication();
        if (auth == null) return null;
        
        Object details = auth.getDetails();
        if (details instanceof JwtAuthenticationDetails jwtAuthenticationDetails) {
            return jwtAuthenticationDetails.getSubscriptionPlan();
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetailsService.CustomUserPrincipal customUserPrincipal) {
            return customUserPrincipal.getSubscriptionPlan();
        }
        
        return null;
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return this;
    }
}