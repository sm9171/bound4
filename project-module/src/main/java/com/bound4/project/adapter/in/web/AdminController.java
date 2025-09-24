package com.bound4.project.adapter.in.web;

import com.bound4.project.adapter.in.web.dto.ApiResponse;
import com.bound4.project.adapter.in.web.dto.admin.*;
import com.bound4.project.application.service.AdminService;
import com.bound4.project.config.security.CustomUserDetailsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("isAdmin()")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/role-policies")
    @PreAuthorize("hasAuthority('MANAGE_POLICIES')")
    public ResponseEntity<ApiResponse<List<RolePolicyResponse>>> getRolePolicies(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal admin) {
        
        log.info("권한 정책 조회 API 호출: adminId={}", admin.getId());
        
        List<RolePolicyResponse> policies = adminService.getRolePolicies(admin.getId());
        
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @PutMapping("/role-policies")
    @PreAuthorize("hasAuthority('MANAGE_POLICIES')")
    public ResponseEntity<ApiResponse<RolePolicyResponse>> updateRolePolicy(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal admin,
            @Valid @RequestBody RolePolicyUpdateRequest request) {
        
        log.info("권한 정책 수정 API 호출: adminId={}, role={}, resource={}, action={}", 
                admin.getId(), request.role(), request.resource(), request.action());
        
        RolePolicyResponse response = adminService.updateRolePolicy(admin.getId(), request);
        
        return ResponseEntity.ok(ApiResponse.success("권한 정책이 성공적으로 수정되었습니다", response));
    }

    @GetMapping("/users/{userId}/permissions")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<ApiResponse<UserPermissionResponse>> getUserPermissions(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal admin,
            @PathVariable Long userId) {
        
        log.info("사용자 권한 조회 API 호출: adminId={}, targetUserId={}", admin.getId(), userId);
        
        UserPermissionResponse response = adminService.getUserPermissions(admin.getId(), userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal admin,
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        
        log.info("사용자 역할 변경 API 호출: adminId={}, targetUserId={}, newRole={}", 
                admin.getId(), userId, request.newRole());
        
        adminService.updateUserRole(admin.getId(), userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("사용자 역할이 성공적으로 변경되었습니다"));
    }

    @PostMapping("/policies/backup")
    @PreAuthorize("hasAuthority('MANAGE_POLICIES')")
    public ResponseEntity<ApiResponse<PolicyBackupResponse>> createPolicyBackup(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal admin) {
        
        log.info("정책 백업 생성 API 호출: adminId={}", admin.getId());
        
        PolicyBackupResponse response = adminService.createPolicyBackup(admin.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("정책 백업이 성공적으로 생성되었습니다", response));
    }

    @GetMapping("/policies/backups")
    @PreAuthorize("hasAuthority('MANAGE_POLICIES')")
    public ResponseEntity<ApiResponse<List<PolicyBackupResponse>>> getPolicyBackups(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal admin) {
        
        log.info("정책 백업 목록 조회 API 호출: adminId={}", admin.getId());
        
        List<PolicyBackupResponse> backups = adminService.getPolicyBackups(admin.getId());
        
        return ResponseEntity.ok(ApiResponse.success(backups));
    }

    @PostMapping("/policies/restore/{backupId}")
    @PreAuthorize("hasAuthority('MANAGE_POLICIES')")
    public ResponseEntity<ApiResponse<Void>> restorePolicyBackup(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal admin,
            @PathVariable String backupId) {
        
        log.info("정책 백업 복원 API 호출: adminId={}, backupId={}", admin.getId(), backupId);
        
        adminService.restorePolicyBackup(admin.getId(), backupId);
        
        return ResponseEntity.ok(ApiResponse.success("정책 백업이 성공적으로 복원되었습니다"));
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAuthority('SYSTEM_ACCESS')")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getDashboardStats(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal admin) {
        
        log.info("관리자 대시보드 통계 조회 API 호출: adminId={}", admin.getId());
        
        // 대시보드 통계 구현 (추후 확장 가능)
        AdminDashboardStats stats = new AdminDashboardStats(
                "관리자 대시보드 통계는 추후 구현 예정입니다"
        );
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    public record AdminDashboardStats(String message) {}
}