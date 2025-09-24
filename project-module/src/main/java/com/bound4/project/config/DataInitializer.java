package com.bound4.project.config;

import com.bound4.project.adapter.out.persistence.PermissionRepository;
import com.bound4.project.adapter.out.persistence.RolePolicyRepository;
import com.bound4.project.adapter.out.persistence.UserRepository;
import com.bound4.project.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RolePolicyRepository rolePolicyRepository;

    public DataInitializer(UserRepository userRepository, 
                          PermissionRepository permissionRepository,
                          RolePolicyRepository rolePolicyRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.rolePolicyRepository = rolePolicyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("RBAC 시스템 데이터 초기화 시작");
        
        initializePermissions();
        initializeRolePolicies();
        initializeDefaultUsers();
        
        log.info("RBAC 시스템 데이터 초기화 완료");
    }

    private void initializePermissions() {
        log.info("기본 권한 데이터 초기화");

        createPermissionsForRole(Role.A, SubscriptionPlan.BASIC);
        createPermissionsForRole(Role.A, SubscriptionPlan.PRO);
        createPermissionsForRole(Role.B, SubscriptionPlan.BASIC);
        createPermissionsForRole(Role.B, SubscriptionPlan.PRO);
        createPermissionsForRole(Role.C, SubscriptionPlan.BASIC);
        createPermissionsForRole(Role.C, SubscriptionPlan.PRO);
        createPermissionsForRole(Role.D, SubscriptionPlan.BASIC);
        createPermissionsForRole(Role.D, SubscriptionPlan.PRO);

        log.info("기본 권한 데이터 초기화 완료: {} 개", permissionRepository.count());
    }

    private void createPermissionsForRole(Role role, SubscriptionPlan plan) {
        for (Resource resource : Resource.values()) {
            for (Action action : Action.values()) {
                if (!permissionRepository.existsByResourceAndActionAndRoleAndSubscriptionPlan(
                        resource, action, role, plan)) {
                    
                    boolean allowed = determinePermission(role, resource, action, plan);
                    Permission permission = new Permission(resource, action, role, plan, allowed);
                    permission.updateDescription(generatePermissionDescription(role, resource, action, plan, allowed));
                    
                    permissionRepository.save(permission);
                }
            }
        }
    }

    private void initializeRolePolicies() {
        log.info("기본 역할 정책 데이터 초기화");

        createRolePoliciesForRole(Role.A, SubscriptionPlan.BASIC);
        createRolePoliciesForRole(Role.A, SubscriptionPlan.PRO);
        createRolePoliciesForRole(Role.B, SubscriptionPlan.BASIC);
        createRolePoliciesForRole(Role.B, SubscriptionPlan.PRO);
        createRolePoliciesForRole(Role.C, SubscriptionPlan.BASIC);
        createRolePoliciesForRole(Role.C, SubscriptionPlan.PRO);
        createRolePoliciesForRole(Role.D, SubscriptionPlan.BASIC);
        createRolePoliciesForRole(Role.D, SubscriptionPlan.PRO);

        log.info("기본 역할 정책 데이터 초기화 완료: {} 개", rolePolicyRepository.count());
    }

    private void createRolePoliciesForRole(Role role, SubscriptionPlan plan) {
        for (Resource resource : Resource.values()) {
            for (Action action : Action.values()) {
                if (!rolePolicyRepository.existsByRoleAndResourceAndActionAndSubscriptionPlan(
                        role, resource, action, plan)) {
                    
                    boolean allowed = determinePermission(role, resource, action, plan);
                    RolePolicy policy = RolePolicy.createSystemPolicy(role, resource, action, allowed, plan);
                    policy.updateReason(generatePolicyReason(role, resource, action, plan, allowed));
                    
                    rolePolicyRepository.save(policy);
                }
            }
        }
    }

    private boolean determinePermission(Role role, Resource resource, Action action, SubscriptionPlan plan) {
        return switch (role) {
            case A -> true; // 관리자는 모든 권한
            
            case B -> switch (resource) { // 프리미엄 사용자
                case PROJECT -> plan == SubscriptionPlan.PRO;
                case USER -> action == Action.READ || action == Action.UPDATE;
                case PERMISSION, ROLE_POLICY -> false;
                case SYSTEM -> action == Action.READ;
            };
            
            case C -> switch (resource) { // 일반 사용자
                case PROJECT -> action == Action.READ || action == Action.CREATE || 
                               (action == Action.UPDATE && plan == SubscriptionPlan.PRO);
                case USER -> action == Action.READ || action == Action.UPDATE;
                case PERMISSION, ROLE_POLICY -> false;
                case SYSTEM -> false;
            };
            
            case D -> switch (resource) { // 기본 사용자
                case PROJECT -> action == Action.READ || 
                               (action == Action.CREATE && plan == SubscriptionPlan.BASIC);
                case USER -> action == Action.READ || action == Action.UPDATE;
                case PERMISSION, ROLE_POLICY, SYSTEM -> false;
            };
        };
    }

    private String generatePermissionDescription(Role role, Resource resource, Action action, 
                                               SubscriptionPlan plan, boolean allowed) {
        String status = allowed ? "허용" : "거부";
        return String.format("%s - %s %s %s (%s)", 
                role.getDescription(), resource.getDescription(), 
                action.getDescription(), status, plan.getDescription());
    }

    private String generatePolicyReason(Role role, Resource resource, Action action, 
                                      SubscriptionPlan plan, boolean allowed) {
        if (allowed) {
            return String.format("%s는 %s에서 %s %s 작업을 수행할 수 있습니다", 
                    role.getDescription(), plan.getDescription(),
                    resource.getDescription(), action.getDescription());
        } else {
            return String.format("%s는 %s에서 %s %s 작업을 수행할 수 없습니다", 
                    role.getDescription(), plan.getDescription(),
                    resource.getDescription(), action.getDescription());
        }
    }

    private void initializeDefaultUsers() {
        log.info("기본 사용자 계정 초기화");

        createUserIfNotExists("admin@bound4.com", "admin123!", Role.A, SubscriptionPlan.PRO);
        createUserIfNotExists("premium@bound4.com", "premium123!", Role.B, SubscriptionPlan.PRO);
        createUserIfNotExists("user@bound4.com", "user123!", Role.C, SubscriptionPlan.BASIC);
        createUserIfNotExists("basic@bound4.com", "basic123!", Role.D, SubscriptionPlan.BASIC);

        log.info("기본 사용자 계정 초기화 완료: {} 개", userRepository.count());
    }

    private void createUserIfNotExists(String email, String password, Role role, SubscriptionPlan plan) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User(email, password, role, plan);
            userRepository.save(user);
            log.info("기본 사용자 생성: {} ({})", email, role.getDescription());
        }
    }
}