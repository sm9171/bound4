package com.bound4.project.application.service;

import com.bound4.project.adapter.in.web.dto.admin.*;
import com.bound4.project.adapter.out.persistence.AuditLogRepository;
import com.bound4.project.adapter.out.persistence.PermissionRepository;
import com.bound4.project.adapter.out.persistence.RolePolicyRepository;
import com.bound4.project.adapter.out.persistence.UserRepository;
import com.bound4.project.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final Map<String, PolicyBackupResponse> policyBackups = new HashMap<>();

    public AdminService(UserRepository userRepository,
                       RolePolicyRepository rolePolicyRepository,
                       PermissionRepository permissionRepository,
                       AuditLogRepository auditLogRepository,
                       NotificationService notificationService,
                       ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.rolePolicyRepository = rolePolicyRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    public List<RolePolicyResponse> getRolePolicies(Long adminUserId) {
        log.debug("권한 정책 조회 요청: adminUserId={}", adminUserId);

        validateAdminUser(adminUserId);

        List<RolePolicy> policies = rolePolicyRepository.findAll();
        return policies.stream()
                .map(RolePolicyResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"permissions", "projectList"}, allEntries = true)
    public RolePolicyResponse updateRolePolicy(Long adminUserId, RolePolicyUpdateRequest request) {
        log.info("권한 정책 수정 요청: adminUserId={}, role={}, resource={}, action={}", 
                adminUserId, request.role(), request.resource(), request.action());

        User adminUser = validateAdminUser(adminUserId);

        Optional<RolePolicy> existingPolicy = rolePolicyRepository
                .findByRoleAndResourceAndActionAndSubscriptionPlan(
                        request.role(), request.resource(), request.action(), request.subscriptionPlan());

        RolePolicy policy;
        String oldValue = null;

        if (existingPolicy.isPresent()) {
            policy = existingPolicy.get();
            if (policy.isSystemPolicy()) {
                throw new AccessDeniedException("시스템 정책은 수정할 수 없습니다");
            }
            
            oldValue = serializePolicyToJson(policy);
            policy.updatePermission(request.allowed(), request.reason());
        } else {
            policy = new RolePolicy(request.role(), request.resource(), request.action(), 
                                  request.allowed(), request.subscriptionPlan());
            policy.updateReason(request.reason());
        }

        RolePolicy savedPolicy = rolePolicyRepository.save(policy);
        String newValue = serializePolicyToJson(savedPolicy);

        // 감사 로그 기록
        AuditLog auditLog = AuditLog.rolePolicyChanged(adminUserId, adminUser.getEmail(),
                savedPolicy.getId(), oldValue, newValue, request.reason());
        setClientInfo(auditLog);
        auditLogRepository.save(auditLog);

        // 영향받는 사용자들에게 알림 전송
        notifyAffectedUsers(request.role(), request.subscriptionPlan(), request.resource(), request.action(), request.allowed(), adminUser.getId());

        log.info("권한 정책 수정 완료: policyId={}, role={}, allowed={}", 
                savedPolicy.getId(), request.role(), request.allowed());

        return RolePolicyResponse.from(savedPolicy);
    }

    public UserPermissionResponse getUserPermissions(Long adminUserId, Long targetUserId) {
        log.debug("사용자 권한 조회 요청: adminUserId={}, targetUserId={}", adminUserId, targetUserId);

        validateAdminUser(adminUserId);
        User targetUser = getUserById(targetUserId);

        List<UserPermissionResponse.PermissionInfo> permissions = new ArrayList<>();
        List<String> authorities = new ArrayList<>();

        // 기본 권한 추가
        authorities.add("ROLE_" + targetUser.getRole().name());
        authorities.add("PLAN_" + targetUser.getSubscriptionPlan().name());

        // 모든 리소스와 액션에 대해 권한 확인
        for (Resource resource : Resource.values()) {
            for (Action action : Action.values()) {
                String source = determinePermissionSource(targetUser, resource, action);
                boolean allowed = hasPermission(targetUser, resource, action);
                
                permissions.add(new UserPermissionResponse.PermissionInfo(
                        resource, action, allowed, source));
            }
        }

        // 역할별 권한 추가
        addRoleBasedAuthorities(targetUser, authorities);

        return UserPermissionResponse.of(targetUser, permissions, authorities);
    }

    @Transactional
    @CacheEvict(value = {"permissions", "projectList"}, allEntries = true)
    public void updateUserRole(Long adminUserId, Long targetUserId, UserRoleUpdateRequest request) {
        log.info("사용자 역할 변경 요청: adminUserId={}, targetUserId={}, newRole={}", 
                adminUserId, targetUserId, request.newRole());

        User adminUser = validateAdminUser(adminUserId);
        User targetUser = getUserById(targetUserId);

        if (targetUser.getRole() == request.newRole()) {
            throw new IllegalArgumentException("이미 동일한 역할입니다: " + request.newRole());
        }

        Role oldRole = targetUser.getRole();
        targetUser.changeRole(request.newRole());
        userRepository.save(targetUser);

        // 감사 로그 기록
        AuditLog auditLog = AuditLog.userRoleChanged(adminUserId, adminUser.getEmail(),
                targetUserId, targetUser.getEmail(), oldRole.name(), request.newRole().name(), request.reason());
        setClientInfo(auditLog);
        auditLogRepository.save(auditLog);

        // 사용자에게 알림 전송
        notificationService.notifyRoleChanged(targetUserId, adminUserId, oldRole.name(), request.newRole().name(), request.reason());

        log.info("사용자 역할 변경 완료: userId={}, oldRole={}, newRole={}", 
                targetUserId, oldRole, request.newRole());
    }

    @Transactional
    public PolicyBackupResponse createPolicyBackup(Long adminUserId) {
        log.info("정책 백업 생성 요청: adminUserId={}", adminUserId);

        User adminUser = validateAdminUser(adminUserId);
        
        List<RolePolicy> allPolicies = rolePolicyRepository.findAll();
        List<RolePolicyResponse> policyResponses = allPolicies.stream()
                .map(RolePolicyResponse::from)
                .toList();

        String backupId = "backup_" + System.currentTimeMillis();
        PolicyBackupResponse backup = new PolicyBackupResponse(
                backupId,
                LocalDateTime.now(),
                adminUserId,
                adminUser.getEmail(),
                allPolicies.size(),
                policyResponses
        );

        policyBackups.put(backupId, backup);

        // 감사 로그 기록
        AuditLog auditLog = AuditLog.policyBackupCreated(adminUserId, adminUser.getEmail(), backupId);
        setClientInfo(auditLog);
        auditLogRepository.save(auditLog);

        log.info("정책 백업 생성 완료: backupId={}, policyCount={}", backupId, allPolicies.size());

        return backup;
    }

    public List<PolicyBackupResponse> getPolicyBackups(Long adminUserId) {
        log.debug("정책 백업 목록 조회 요청: adminUserId={}", adminUserId);

        validateAdminUser(adminUserId);
        return new ArrayList<>(policyBackups.values());
    }

    @Transactional
    @CacheEvict(value = {"permissions", "projectList"}, allEntries = true)
    public void restorePolicyBackup(Long adminUserId, String backupId) {
        log.info("정책 백업 복원 요청: adminUserId={}, backupId={}", adminUserId, backupId);

        User adminUser = validateAdminUser(adminUserId);
        PolicyBackupResponse backup = policyBackups.get(backupId);
        
        if (backup == null) {
            throw new EntityNotFoundException("백업을 찾을 수 없습니다: " + backupId);
        }

        // 현재 커스텀 정책들 삭제 (시스템 정책 제외)
        List<RolePolicy> customPolicies = rolePolicyRepository.findCustomPolicies();
        rolePolicyRepository.deleteAll(customPolicies);

        // 백업된 정책들 복원 (시스템 정책 제외)
        List<RolePolicy> policiesToRestore = backup.policies().stream()
                .filter(p -> !p.systemPolicy())
                .map(this::convertToRolePolicy)
                .toList();

        rolePolicyRepository.saveAll(policiesToRestore);

        // 감사 로그 기록
        AuditLog auditLog = AuditLog.userRoleChanged(adminUserId, adminUser.getEmail(), null, null,
                "BACKUP_RESTORED", backupId, "정책 백업 복원");
        setClientInfo(auditLog);
        auditLogRepository.save(auditLog);

        log.info("정책 백업 복원 완료: backupId={}, restoredCount={}", backupId, policiesToRestore.size());
    }

    private User validateAdminUser(Long adminUserId) {
        User user = getUserById(adminUserId);
        if (user.getRole() != Role.A) {
            throw new AccessDeniedException("관리자 권한이 필요합니다");
        }
        return user;
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    private String determinePermissionSource(User user, Resource resource, Action action) {
        // 역할 정책 확인
        Optional<RolePolicy> policy = rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                user.getRole(), resource, action, user.getSubscriptionPlan());
        if (policy.isPresent()) {
            return "ROLE_POLICY";
        }

        // 기본 권한 확인
        Optional<Permission> permission = permissionRepository.findByResourceAndActionAndRoleAndSubscriptionPlan(
                resource, action, user.getRole(), user.getSubscriptionPlan());
        if (permission.isPresent()) {
            return "PERMISSION";
        }

        return "DEFAULT";
    }

    private boolean hasPermission(User user, Resource resource, Action action) {
        // 역할 정책 우선 확인
        Optional<RolePolicy> policy = rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                user.getRole(), resource, action, user.getSubscriptionPlan());
        if (policy.isPresent()) {
            return policy.get().isAllowed();
        }

        // 기본 권한 확인
        Optional<Permission> permission = permissionRepository.findByResourceAndActionAndRoleAndSubscriptionPlan(
                resource, action, user.getRole(), user.getSubscriptionPlan());
        if (permission.isPresent()) {
            return permission.get().isAllowed();
        }

        // 역할 기반 기본 로직
        return user.hasPermission(resource.name(), action.name());
    }

    private void addRoleBasedAuthorities(User user, List<String> authorities) {
        switch (user.getRole()) {
            case A -> {
                authorities.add("ADMIN");
                authorities.add("MANAGE_USERS");
                authorities.add("MANAGE_PERMISSIONS");
                authorities.add("MANAGE_POLICIES");
                authorities.add("SYSTEM_ACCESS");
            }
            case B -> {
                authorities.add("PREMIUM_USER");
                authorities.add("ADVANCED_FEATURES");
                if (user.getSubscriptionPlan() == SubscriptionPlan.PRO) {
                    authorities.add("PRO_FEATURES");
                }
            }
            case C -> {
                authorities.add("STANDARD_USER");
                if (user.getSubscriptionPlan() == SubscriptionPlan.PRO) {
                    authorities.add("ENHANCED_FEATURES");
                }
            }
            case D -> authorities.add("BASIC_USER");
        }
    }

    private String serializePolicyToJson(RolePolicy policy) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("role", policy.getRole());
            data.put("resource", policy.getResource());
            data.put("action", policy.getAction());
            data.put("allowed", policy.isAllowed());
            data.put("subscriptionPlan", policy.getSubscriptionPlan());
            data.put("reason", policy.getReason());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "JSON 직렬화 실패: " + e.getMessage();
        }
    }

    private RolePolicy convertToRolePolicy(RolePolicyResponse response) {
        RolePolicy policy = new RolePolicy(response.role(), response.resource(), 
                response.action(), response.allowed(), response.subscriptionPlan());
        policy.updateReason(response.reason());
        return policy;
    }

    private void setClientInfo(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            auditLog.setClientInfo(clientIp, userAgent);
        } catch (Exception e) {
            log.warn("클라이언트 정보 설정 실패: {}", e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    private void notifyAffectedUsers(Role role, SubscriptionPlan subscriptionPlan, Resource resource, Action action, boolean allowed, Long adminUserId) {
        List<User> affectedUsers = userRepository.findByRoleAndSubscriptionPlan(role, subscriptionPlan);
        
        for (User user : affectedUsers) {
            notificationService.notifyPermissionChanged(user.getId(), adminUserId, 
                    resource.getDescription(), action.getDescription(), allowed);
        }
        
        log.info("권한 변경 알림 전송 완료: affectedUserCount={}, role={}, resource={}, action={}", 
                affectedUsers.size(), role, resource, action);
    }
}