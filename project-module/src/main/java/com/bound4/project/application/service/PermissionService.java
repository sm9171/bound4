package com.bound4.project.application.service;

import com.bound4.project.adapter.out.persistence.PermissionRepository;
import com.bound4.project.adapter.out.persistence.ProjectRepository;
import com.bound4.project.adapter.out.persistence.RolePolicyRepository;
import com.bound4.project.adapter.out.persistence.UserRepository;
import com.bound4.project.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PermissionRepository permissionRepository;
    private final RolePolicyRepository rolePolicyRepository;

    public PermissionService(UserRepository userRepository,
                           ProjectRepository projectRepository,
                           PermissionRepository permissionRepository,
                           RolePolicyRepository rolePolicyRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.permissionRepository = permissionRepository;
        this.rolePolicyRepository = rolePolicyRepository;
    }

    public boolean checkProjectAccess(Long userId, String action, SubscriptionPlan subscriptionPlan) {
        User user = getUserById(userId);
        Action actionEnum = Action.valueOf(action.toUpperCase());
        
        return hasPermission(user, Resource.PROJECT, actionEnum, subscriptionPlan);
    }

    public boolean checkProjectAccess(Long userId, Long projectId, String action) {
        User user = getUserById(userId);
        Project project = getProjectById(projectId);
        Action actionEnum = Action.valueOf(action.toUpperCase());

        // 프로젝트 소유자는 모든 권한 허용 (삭제 제외는 역할에 따라)
        if (project.isOwnedBy(user)) {
            if (actionEnum == Action.DELETE && !canDeleteProject(user)) {
                return false;
            }
            return true;
        }

        // 소유자가 아닌 경우 역할 기반 권한 검사
        return hasPermission(user, Resource.PROJECT, actionEnum, user.getSubscriptionPlan()) &&
               project.canBeAccessedBy(user);
    }

    public boolean hasPermission(Long userId, String resource, String action) {
        User user = getUserById(userId);
        Resource resourceEnum = Resource.valueOf(resource.toUpperCase());
        Action actionEnum = Action.valueOf(action.toUpperCase());
        
        return hasPermission(user, resourceEnum, actionEnum, user.getSubscriptionPlan());
    }

    public boolean hasPermission(User user, Resource resource, Action action, SubscriptionPlan subscriptionPlan) {
        // 1. 역할 정책 우선 확인 (관리자가 설정 가능)
        Optional<RolePolicy> policy = rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                user.getRole(), resource, action, subscriptionPlan);
        
        if (policy.isPresent()) {
            boolean allowed = policy.get().isAllowed();
            log.debug("역할 정책 적용: 사용자={}, 리소스={}, 액션={}, 허용={}", 
                    user.getEmail(), resource, action, allowed);
            return allowed;
        }

        // 2. 기본 권한 확인
        Optional<Permission> permission = permissionRepository.findByResourceAndActionAndRoleAndSubscriptionPlan(
                resource, action, user.getRole(), subscriptionPlan);
        
        if (permission.isPresent()) {
            boolean allowed = permission.get().isAllowed();
            log.debug("기본 권한 적용: 사용자={}, 리소스={}, 액션={}, 허용={}", 
                    user.getEmail(), resource, action, allowed);
            return allowed;
        }

        // 3. 역할 기반 기본 로직
        boolean allowed = user.hasPermission(resource.name(), action.name());
        log.debug("역할 기반 권한 적용: 사용자={}, 리소스={}, 액션={}, 허용={}", 
                user.getEmail(), resource, action, allowed);
        return allowed;
    }

    public void validateSubscriptionLimit(Long userId, SubscriptionPlan requiredPlan) {
        User user = getUserById(userId);
        
        if (!user.getSubscriptionPlan().isUpgradeFrom(requiredPlan) && 
            user.getSubscriptionPlan() != requiredPlan) {
            throw new AccessDeniedException(
                String.format("이 기능을 사용하려면 %s 플랜 이상이 필요합니다", requiredPlan.getDescription()));
        }
    }

    public void validateProjectCreationLimit(Long userId) {
        User user = getUserById(userId);
        
        long currentProjectCount = projectRepository.countByUserAndStatus(user, ProjectStatus.ACTIVE);
        
        if (!user.getSubscriptionPlan().canCreateProject((int) currentProjectCount)) {
            throw new AccessDeniedException(
                String.format("현재 플랜(%s)에서는 최대 %d개의 프로젝트만 생성할 수 있습니다", 
                        user.getSubscriptionPlan().getDescription(),
                        user.getSubscriptionPlan().getMaxProjects()));
        }
    }

    public void enforceProjectAccess(Long userId, Long projectId, String action) {
        if (!checkProjectAccess(userId, projectId, action)) {
            User user = getUserById(userId);
            throw new AccessDeniedException(
                String.format("사용자 %s는 프로젝트 %d에 대한 %s 권한이 없습니다", 
                        user.getEmail(), projectId, action));
        }
    }

    public void enforcePermission(Long userId, String resource, String action) {
        if (!hasPermission(userId, resource, action)) {
            User user = getUserById(userId);
            throw new AccessDeniedException(
                String.format("사용자 %s는 %s 리소스에 대한 %s 권한이 없습니다", 
                        user.getEmail(), resource, action));
        }
    }

    public List<Resource> getAllowedResources(Long userId) {
        User user = getUserById(userId);
        return rolePolicyRepository.findAllowedResourcesByRoleAndPlan(user.getRole(), user.getSubscriptionPlan());
    }

    public List<Action> getAllowedActions(Long userId, String resource) {
        User user = getUserById(userId);
        Resource resourceEnum = Resource.valueOf(resource.toUpperCase());
        return rolePolicyRepository.findAllowedActionsByRoleAndPlanAndResource(
                user.getRole(), user.getSubscriptionPlan(), resourceEnum);
    }

    @Transactional
    public void updateRolePolicy(Long userId, Role role, String resource, String action, 
                               boolean allowed, String reason, SubscriptionPlan subscriptionPlan) {
        // 관리자 권한 확인
        enforcePermission(userId, "ROLE_POLICY", "MANAGE");
        
        Resource resourceEnum = Resource.valueOf(resource.toUpperCase());
        Action actionEnum = Action.valueOf(action.toUpperCase());
        
        Optional<RolePolicy> existingPolicy = rolePolicyRepository
                .findByRoleAndResourceAndActionAndSubscriptionPlan(role, resourceEnum, actionEnum, subscriptionPlan);
        
        if (existingPolicy.isPresent()) {
            RolePolicy policy = existingPolicy.get();
            if (policy.canBeModified()) {
                policy.updatePermission(allowed, reason);
                rolePolicyRepository.save(policy);
                log.info("역할 정책 업데이트: {} {} {} -> {}", role, resource, action, allowed);
            } else {
                throw new AccessDeniedException("시스템 정책은 수정할 수 없습니다");
            }
        } else {
            RolePolicy newPolicy = new RolePolicy(role, resourceEnum, actionEnum, allowed, subscriptionPlan);
            newPolicy.updateReason(reason);
            rolePolicyRepository.save(newPolicy);
            log.info("새 역할 정책 생성: {} {} {} -> {}", role, resource, action, allowed);
        }
    }

    private boolean canDeleteProject(User user) {
        return user.getRole() == Role.A || user.getRole() == Role.B;
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("사용자를 찾을 수 없습니다: " + userId));
    }

    private Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new AccessDeniedException("프로젝트를 찾을 수 없습니다: " + projectId));
    }
}