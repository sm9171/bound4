package com.bound4.project.application.service;

import com.bound4.project.adapter.in.web.dto.admin.*;
import com.bound4.project.adapter.out.persistence.*;
import com.bound4.project.config.TestDataConfig;
import com.bound4.project.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 단위 테스트")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolePolicyRepository rolePolicyRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminService adminService;

    private TestDataConfig.TestUserFactory userFactory;
    private TestDataConfig.TestRolePolicyFactory policyFactory;
    private User adminUser;
    private User standardUser;
    private RolePolicy testPolicy;

    @BeforeEach
    void setUp() {
        userFactory = new TestDataConfig.TestUserFactory();
        policyFactory = new TestDataConfig.TestRolePolicyFactory();
        adminUser = userFactory.createAdminUser();
        standardUser = userFactory.createStandardUser();
        testPolicy = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);
        
        // Mock 테스트를 위한 ID 설정
        setUserId(adminUser, 1L);
        setUserId(standardUser, 2L);
    }
    
    private void setUserId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user ID", e);
        }
    }

    @Test
    @DisplayName("관리자는 권한 정책을 조회할 수 있다")
    void getRolePolicies_WithAdmin_ShouldSucceed() {
        // given
        Long adminId = 1L;
        List<RolePolicy> policies = Arrays.asList(testPolicy);

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        given(rolePolicyRepository.findAll()).willReturn(policies);

        // when
        List<RolePolicyResponse> response = adminService.getRolePolicies(adminId);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).role()).isEqualTo(Role.C);
        assertThat(response.get(0).resource()).isEqualTo(Resource.PROJECT);
        assertThat(response.get(0).action()).isEqualTo(Action.CREATE);
    }

    @Test
    @DisplayName("관리자가 아닌 사용자가 권한 정책을 조회하면 예외가 발생한다")
    void getRolePolicies_WithNonAdmin_ShouldThrowException() {
        // given
        Long userId = 1L;

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));

        // when & then
        assertThatThrownBy(() -> adminService.getRolePolicies(userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("관리자 권한이 필요합니다");
    }

    @Test
    @DisplayName("관리자는 권한 정책을 수정할 수 있다")
    void updateRolePolicy_WithValidRequest_ShouldSucceed() {
        // given
        Long adminId = 1L;
        RolePolicyUpdateRequest request = new RolePolicyUpdateRequest(
                Role.C, Resource.PROJECT, Action.CREATE, false, SubscriptionPlan.BASIC, "테스트 수정"
        );

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        given(rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC))
                .willReturn(Optional.of(testPolicy));
        given(rolePolicyRepository.save(testPolicy)).willReturn(testPolicy);
        try {
            given(objectMapper.writeValueAsString(any())).willReturn("test_json");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        given(userRepository.findByRoleAndSubscriptionPlan(Role.C, SubscriptionPlan.BASIC))
                .willReturn(Arrays.asList(standardUser));

        // when
        RolePolicyResponse response = adminService.updateRolePolicy(adminId, request);

        // then
        assertThat(response.allowed()).isFalse();
        assertThat(response.reason()).isEqualTo("테스트 수정");

        verify(auditLogRepository).save(any(AuditLog.class));
        verify(notificationService).notifyPermissionChanged(eq(2L), eq(adminId), anyString(), anyString(), eq(false));
    }

    @Test
    @DisplayName("시스템 정책을 수정하려고 하면 예외가 발생한다")
    void updateRolePolicy_WithSystemPolicy_ShouldThrowException() {
        // given
        Long adminId = 1L;
        RolePolicy systemPolicy = policyFactory.createSystemPolicy(Role.A, Resource.SYSTEM, Action.MANAGE, true, SubscriptionPlan.PRO);
        RolePolicyUpdateRequest request = new RolePolicyUpdateRequest(
                Role.A, Resource.SYSTEM, Action.MANAGE, false, SubscriptionPlan.PRO, "시스템 정책 수정 시도"
        );

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        given(rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                Role.A, Resource.SYSTEM, Action.MANAGE, SubscriptionPlan.PRO))
                .willReturn(Optional.of(systemPolicy));

        // when & then
        assertThatThrownBy(() -> adminService.updateRolePolicy(adminId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("시스템 정책은 수정할 수 없습니다");

        verify(rolePolicyRepository, never()).save(any(RolePolicy.class));
    }

    @Test
    @DisplayName("관리자는 사용자 권한을 조회할 수 있다")
    void getUserPermissions_WithValidUser_ShouldSucceed() {
        // given
        Long adminId = 1L;
        Long targetUserId = 2L;

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(standardUser));

        // when
        UserPermissionResponse response = adminService.getUserPermissions(adminId, targetUserId);

        // then
        assertThat(response.user().id()).isEqualTo(standardUser.getId());
        assertThat(response.user().email()).isEqualTo(standardUser.getEmail());
        assertThat(response.user().role()).isEqualTo(standardUser.getRole());
        assertThat(response.permissions()).isNotEmpty();
        assertThat(response.authorities()).contains("ROLE_C", "PLAN_BASIC", "STANDARD_USER");
    }

    @Test
    @DisplayName("관리자는 사용자 역할을 변경할 수 있다")
    void updateUserRole_WithValidRequest_ShouldSucceed() {
        // given
        Long adminId = 1L;
        Long targetUserId = 2L;
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.B, "테스트 역할 변경");

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(standardUser));
        given(userRepository.save(standardUser)).willReturn(standardUser);

        // when
        adminService.updateUserRole(adminId, targetUserId, request);

        // then
        assertThat(standardUser.getRole()).isEqualTo(Role.B);

        verify(userRepository).save(standardUser);
        verify(auditLogRepository).save(any(AuditLog.class));
        verify(notificationService).notifyRoleChanged(targetUserId, adminId, "C", "B", "테스트 역할 변경");
    }

    @Test
    @DisplayName("같은 역할로 변경하려고 하면 예외가 발생한다")
    void updateUserRole_WithSameRole_ShouldThrowException() {
        // given
        Long adminId = 1L;
        Long targetUserId = 2L;
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.C, "같은 역할로 변경");

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(standardUser));

        // when & then
        assertThatThrownBy(() -> adminService.updateUserRole(adminId, targetUserId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 동일한 역할입니다");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("관리자는 정책 백업을 생성할 수 있다")
    void createPolicyBackup_WithAdmin_ShouldSucceed() {
        // given
        Long adminId = 1L;
        List<RolePolicy> policies = Arrays.asList(testPolicy);

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        given(rolePolicyRepository.findAll()).willReturn(policies);

        // when
        PolicyBackupResponse response = adminService.createPolicyBackup(adminId);

        // then
        assertThat(response.backupId()).startsWith("backup_");
        assertThat(response.adminUserId()).isEqualTo(adminId);
        assertThat(response.adminEmail()).isEqualTo(adminUser.getEmail());
        assertThat(response.policyCount()).isEqualTo(1);
        assertThat(response.policies()).hasSize(1);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("관리자는 정책 백업을 복원할 수 있다")
    void restorePolicyBackup_WithValidBackup_ShouldSucceed() {
        // given
        Long adminId = 1L;
        
        // 먼저 백업을 생성
        List<RolePolicy> policies = Arrays.asList(testPolicy);
        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        given(rolePolicyRepository.findAll()).willReturn(policies);
        PolicyBackupResponse backup = adminService.createPolicyBackup(adminId);
        
        given(rolePolicyRepository.findCustomPolicies()).willReturn(Arrays.asList());

        // when & then
        assertThatCode(() -> adminService.restorePolicyBackup(adminId, backup.backupId()))
                .doesNotThrowAnyException();

        verify(auditLogRepository, atLeast(2)).save(any(AuditLog.class)); // 백업 생성 + 복원
    }

    @Test
    @DisplayName("존재하지 않는 백업을 복원하려고 하면 예외가 발생한다")
    void restorePolicyBackup_WithNonExistentBackup_ShouldThrowException() {
        // given
        Long adminId = 1L;
        String nonExistentBackupId = "backup_nonexistent";

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));

        // when & then
        assertThatThrownBy(() -> adminService.restorePolicyBackup(adminId, nonExistentBackupId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("백업을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 관리자 ID로 요청하면 예외가 발생한다")
    void getRolePolicies_WithNonExistentAdmin_ShouldThrowException() {
        // given
        Long nonExistentAdminId = 999L;

        given(userRepository.findById(nonExistentAdminId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.getRolePolicies(nonExistentAdminId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}