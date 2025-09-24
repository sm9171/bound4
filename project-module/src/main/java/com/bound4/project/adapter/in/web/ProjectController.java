package com.bound4.project.adapter.in.web;

import com.bound4.project.adapter.in.web.dto.*;
import com.bound4.project.application.service.ProjectService;
import com.bound4.project.config.security.CustomUserDetailsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("canCreateProject()")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal user,
            @Valid @RequestBody ProjectCreateRequest request) {
        
        log.info("프로젝트 생성 API 호출: userId={}, name={}", user.getId(), request.name());
        
        ProjectResponse response = projectService.createProject(user.getId(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("프로젝트가 성공적으로 생성되었습니다", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('A', 'D') and hasProjectPermission(#id, 'READ')")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal user,
            @PathVariable Long id) {
        
        log.info("프로젝트 조회 API 호출: userId={}, projectId={}", user.getId(), id);
        
        ProjectResponse response = projectService.getProject(user.getId(), id);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('A', 'B') and hasProjectPermission(#id, 'UPDATE')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        
        log.info("프로젝트 수정 API 호출: userId={}, projectId={}, name={}", 
                user.getId(), id, request.name());
        
        ProjectResponse response = projectService.updateProject(user.getId(), id, request);
        
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 성공적으로 수정되었습니다", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('A', 'B', 'C') and hasProjectPermission(#id, 'DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal user,
            @PathVariable Long id) {
        
        log.info("프로젝트 삭제 API 호출: userId={}, projectId={}", user.getId(), id);
        
        projectService.deleteProject(user.getId(), id);
        
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 성공적으로 삭제되었습니다"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('A', 'D') and hasResourcePermission('PROJECT', 'READ')")
    public ResponseEntity<ApiResponse<ProjectListResponse>> getProjects(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("프로젝트 목록 조회 API 호출: userId={}, page={}, size={}", 
                user.getId(), pageable.getPageNumber(), pageable.getPageSize());
        
        Page<ProjectResponse> projects = projectService.getProjects(user.getId(), pageable);
        ProjectListResponse response = ProjectListResponse.from(projects);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/count")
    @PreAuthorize("hasResourcePermission('PROJECT', 'READ')")
    public ResponseEntity<ApiResponse<ProjectCountResponse>> getProjectCount(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal user) {
        
        log.info("프로젝트 개수 조회 API 호출: userId={}", user.getId());
        
        long count = projectService.getActiveProjectCount(user.getId());
        ProjectCountResponse response = new ProjectCountResponse(count);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    public record ProjectCountResponse(long count) {}
}