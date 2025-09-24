package com.bound4.project.config;

import com.bound4.project.domain.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestDataConfig {

    @Bean
    @Primary
    public TestUserFactory testUserFactory() {
        return new TestUserFactory();
    }

    @Bean
    @Primary
    public TestProjectFactory testProjectFactory() {
        return new TestProjectFactory();
    }

    @Bean
    @Primary
    public TestRolePolicyFactory testRolePolicyFactory() {
        return new TestRolePolicyFactory();
    }

    public static class TestUserFactory {
        
        public User createAdminUser() {
            return new User("admin@test.com", "admin123!", Role.A, SubscriptionPlan.PRO);
        }

        public User createPremiumUser() {
            return new User("premium@test.com", "premium123!", Role.B, SubscriptionPlan.PRO);
        }

        public User createStandardUser() {
            return new User("standard@test.com", "standard123!", Role.C, SubscriptionPlan.BASIC);
        }

        public User createBasicUser() {
            return new User("basic@test.com", "basic123!", Role.D, SubscriptionPlan.BASIC);
        }

        public User createUserWithRole(Role role, SubscriptionPlan plan) {
            String email = role.name().toLowerCase() + "@test.com";
            return new User(email, "password123!", role, plan);
        }
    }

    public static class TestProjectFactory {
        
        public Project createProject(String name, String description, User user) {
            return new Project(name, description, user);
        }

        public Project createTestProject(User user) {
            return new Project("Test Project", "Test Description", user);
        }
    }

    public static class TestRolePolicyFactory {
        
        public RolePolicy createAllowedPolicy(Role role, Resource resource, Action action, SubscriptionPlan plan) {
            return new RolePolicy(role, resource, action, true, plan);
        }

        public RolePolicy createDeniedPolicy(Role role, Resource resource, Action action, SubscriptionPlan plan) {
            return new RolePolicy(role, resource, action, false, plan);
        }

        public RolePolicy createSystemPolicy(Role role, Resource resource, Action action, boolean allowed, SubscriptionPlan plan) {
            return RolePolicy.createSystemPolicy(role, resource, action, allowed, plan);
        }
    }

    public static class TestAuditLogFactory {
        
        public static AuditLog createRolePolicyChangeLog(Long adminUserId, String adminEmail, Long policyId) {
            return AuditLog.rolePolicyChanged(adminUserId, adminEmail, policyId, 
                    "old_value", "new_value", "test_reason");
        }

        public static AuditLog createUserRoleChangeLog(Long adminUserId, String adminEmail, Long targetUserId, String targetEmail) {
            return AuditLog.userRoleChanged(adminUserId, adminEmail, targetUserId, targetEmail, 
                    "OLD_ROLE", "NEW_ROLE", "test_reason");
        }
    }

    public static class TestNotificationFactory {
        
        public static UserNotification createRoleChangeNotification(Long userId) {
            return UserNotification.roleChanged(userId, 1L, "ROLE_C", "ROLE_B", "테스트 역할 변경");
        }

        public static UserNotification createPermissionChangeNotification(Long userId) {
            return UserNotification.permissionChanged(userId, 1L, "PROJECT", "CREATE", true);
        }

        public static UserNotification createSystemNotification(Long userId) {
            return UserNotification.systemNotice(userId, "시스템 공지", "테스트 시스템 공지입니다");
        }
    }
}