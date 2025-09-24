package com.bound4.project.application.service;

import com.bound4.project.adapter.in.web.dto.ProjectCreateRequest;
import com.bound4.project.adapter.in.web.dto.ProjectResponse;
import com.bound4.project.adapter.in.web.dto.ProjectUpdateRequest;
import com.bound4.project.adapter.out.persistence.ProjectRepository;
import com.bound4.project.adapter.out.persistence.UserRepository;
import com.bound4.project.domain.Project;
import com.bound4.project.domain.ProjectStatus;
import com.bound4.project.domain.User;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public ProjectService(ProjectRepository projectRepository,
                         UserRepository userRepository,
                         PermissionService permissionService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectResponse createProject(Long userId, ProjectCreateRequest request) {
        log.debug("프로젝트 생성 요청: userId={}, name={}", userId, request.name());

        // 구독 플랜별 생성 제한 검증
        permissionService.validateProjectCreationLimit(userId);

        User user = getUserById(userId);
        
        // 프로젝트 생성 권한 검증
        permissionService.enforcePermission(userId, "PROJECT", "CREATE");

        // 같은 이름의 프로젝트가 이미 존재하는지 확인
        if (projectRepository.existsByUserAndName(user, request.name())) {
            throw new IllegalArgumentException("같은 이름의 프로젝트가 이미 존재합니다: " + request.name());
        }

        Project project = new Project(request.name(), request.description(), user);
        Project savedProject = projectRepository.save(project);

        log.info("프로젝트 생성 완료: id={}, name={}, userId={}", 
                savedProject.getId(), savedProject.getName(), userId);

        return ProjectResponse.from(savedProject);
    }

    @Cacheable(value = "projects", key = "#projectId", condition = "#projectId != null")
    public ProjectResponse getProject(Long userId, Long projectId) {
        log.debug("프로젝트 조회 요청: userId={}, projectId={}", userId, projectId);

        Project project = getProjectById(projectId);
        
        // 프로젝트 접근 권한 검증
        permissionService.enforceProjectAccess(userId, projectId, "READ");

        return ProjectResponse.from(project);
    }

    @Transactional
    @CacheEvict(value = "projects", key = "#projectId")
    public ProjectResponse updateProject(Long userId, Long projectId, ProjectUpdateRequest request) {
        log.debug("프로젝트 수정 요청: userId={}, projectId={}, name={}", 
                userId, projectId, request.name());

        Project project = getProjectById(projectId);
        
        // 프로젝트 수정 권한 검증
        permissionService.enforceProjectAccess(userId, projectId, "UPDATE");

        // 업데이트할 필드가 있는지 확인
        boolean hasChanges = false;

        if (request.name() != null && !request.name().equals(project.getName())) {
            // 같은 사용자의 다른 프로젝트 중 같은 이름이 있는지 확인
            if (projectRepository.existsByUserAndName(project.getUser(), request.name())) {
                throw new IllegalArgumentException("같은 이름의 프로젝트가 이미 존재합니다: " + request.name());
            }
            project.updateName(request.name());
            hasChanges = true;
        }

        if (request.description() != null && !request.description().equals(project.getDescription())) {
            project.updateDescription(request.description());
            hasChanges = true;
        }

        if (!hasChanges) {
            log.debug("프로젝트 수정 사항 없음: projectId={}", projectId);
            return ProjectResponse.from(project);
        }

        Project updatedProject = projectRepository.save(project);

        log.info("프로젝트 수정 완료: id={}, name={}, userId={}", 
                updatedProject.getId(), updatedProject.getName(), userId);

        return ProjectResponse.from(updatedProject);
    }

    @Transactional
    @CacheEvict(value = "projects", key = "#projectId")
    public void deleteProject(Long userId, Long projectId) {
        log.debug("프로젝트 삭제 요청: userId={}, projectId={}", userId, projectId);

        Project project = getProjectById(projectId);
        
        // 프로젝트 삭제 권한 검증
        permissionService.enforceProjectAccess(userId, projectId, "DELETE");

        // 소프트 삭제 (상태 변경)
        project.archive();
        projectRepository.save(project);

        log.info("프로젝트 삭제 완료: id={}, name={}, userId={}", 
                project.getId(), project.getName(), userId);
    }

    @Cacheable(value = "projectList", key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProjectResponse> getProjects(Long userId, Pageable pageable) {
        log.debug("프로젝트 목록 조회 요청: userId={}, page={}, size={}", 
                userId, pageable.getPageNumber(), pageable.getPageSize());

        User user = getUserById(userId);
        
        // 프로젝트 목록 조회 권한 검증
        permissionService.enforcePermission(userId, "PROJECT", "READ");

        Page<Project> projects;
        
        // 관리자는 모든 프로젝트 조회 가능, 일반 사용자는 본인 프로젝트만
        if (user.getRole().name().equals("A")) {
            projects = projectRepository.findByStatus(ProjectStatus.ACTIVE, pageable);
        } else {
            projects = projectRepository.findByUserAndStatus(user, ProjectStatus.ACTIVE, pageable);
        }

        return projects.map(ProjectResponse::from);
    }

    public long getActiveProjectCount(Long userId) {
        User user = getUserById(userId);
        return projectRepository.countByUserAndStatus(user, ProjectStatus.ACTIVE);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    private Project getProjectById(Long projectId) {
        return projectRepository.findByIdWithUser(projectId)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
    }
}