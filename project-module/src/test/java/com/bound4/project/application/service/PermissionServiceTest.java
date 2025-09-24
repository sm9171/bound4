package com.bound4.project.application.service;

import com.bound4.project.adapter.out.persistence.*;
import com.bound4.project.config.TestDataConfig;
import com.bound4.project.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
@DisplayName("PermissionService 단위 테스트")
class PermissionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePolicyRepository rolePolicyRepository;

    @InjectMocks
    private PermissionService permissionService;

    private TestDataConfig.TestUserFactory userFactory;
    private TestDataConfig.TestProjectFactory projectFactory;
    private TestDataConfig.TestRolePolicyFactory policyFactory;

    private User adminUser;
    private User premiumUser;
    private User standardUser;
    private User basicUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        userFactory = new TestDataConfig.TestUserFactory();
        projectFactory = new TestDataConfig.TestProjectFactory();
        policyFactory = new TestDataConfig.TestRolePolicyFactory();

        adminUser = userFactory.createAdminUser();
        premiumUser = userFactory.createPremiumUser();
        standardUser = userFactory.createStandardUser();
        basicUser = userFactory.createBasicUser();
        testProject = projectFactory.createTestProject(standardUser);
        
        // Mock 테스트를 위한 ID 설정
        setUserId(adminUser, 1L);
        setUserId(premiumUser, 2L);
        setUserId(standardUser, 3L);
        setUserId(basicUser, 4L);
        setProjectId(testProject, 1L);
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
    
    private void setProjectId(Project project, Long id) {
        try {
            java.lang.reflect.Field idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set project ID", e);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "A, PROJECT, CREATE, PRO, true",
            "A, USER, DELETE, BASIC, true",
            "B, PROJECT, READ, PRO, true",
            "C, PROJECT, CREATE, BASIC, true",
            "D, PROJECT, READ, BASIC, true",
            "D, PROJECT, DELETE, BASIC, false"
    })
    @DisplayName("구독 플랜별 프로젝트 접근 권한 검증이 올바르게 작동한다")
    void checkProjectAccess_ShouldReturnCorrectResult(Role role, String resource, String action, 
                                                     SubscriptionPlan plan, boolean expected) {
        // given
        User user = userFactory.createUserWithRole(role, plan);
        Long userId = 1L;

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        boolean result = permissionService.checkProjectAccess(userId, action, plan);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("프로젝트 소유자는 모든 권한을 가진다 (삭제 제외)")
    void checkProjectAccess_WithOwner_ShouldAllowMostActions() {
        // given
        Long userId = 1L;
        Long projectId = 1L;

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));
        given(projectRepository.findById(projectId)).willReturn(Optional.of(testProject));

        // when & then
        assertThat(permissionService.checkProjectAccess(userId, projectId, "READ")).isTrue();
        assertThat(permissionService.checkProjectAccess(userId, projectId, "UPDATE")).isTrue();
    }

    @Test
    @DisplayName("기본 사용자(D 역할)는 프로젝트를 삭제할 수 없다")
    void checkProjectAccess_WithBasicUserDelete_ShouldReturnFalse() {
        // given
        Long basicUserId = 1L;
        Project basicUserProject = projectFactory.createTestProject(basicUser);
        Long projectId = 1L;

        given(userRepository.findById(basicUserId)).willReturn(Optional.of(basicUser));
        given(projectRepository.findById(projectId)).willReturn(Optional.of(basicUserProject));

        // when
        boolean result = permissionService.checkProjectAccess(basicUserId, projectId, "DELETE");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("역할 정책이 존재하면 우선 적용된다")
    void hasPermission_WithRolePolicy_ShouldUsePolicyFirst() {
        // given
        Long userId = 1L;
        RolePolicy allowPolicy = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.DELETE, SubscriptionPlan.BASIC);

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));
        given(rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                Role.C, Resource.PROJECT, Action.DELETE, SubscriptionPlan.BASIC))
                .willReturn(Optional.of(allowPolicy));

        // when
        boolean result = permissionService.hasPermission(userId, "PROJECT", "DELETE");

        // then
        assertThat(result).isTrue();
        verify(permissionRepository, never()).findByResourceAndActionAndRoleAndSubscriptionPlan(any(), any(), any(), any());
    }

    @Test
    @DisplayName("기본 권한이 존재하면 두 번째로 적용된다")
    void hasPermission_WithPermission_ShouldUsePermissionSecond() {
        // given
        Long userId = 1L;
        Permission allowPermission = new Permission(Resource.PROJECT, Action.CREATE, Role.C, SubscriptionPlan.BASIC, true);

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));
        given(rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(permissionRepository.findByResourceAndActionAndRoleAndSubscriptionPlan(
                Resource.PROJECT, Action.CREATE, Role.C, SubscriptionPlan.BASIC))
                .willReturn(Optional.of(allowPermission));

        // when
        boolean result = permissionService.hasPermission(userId, "PROJECT", "CREATE");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("구독 플랜 제한을 검증할 수 있다")
    void validateSubscriptionLimit_WithValidPlan_ShouldPass() {
        // given
        Long userId = 1L;
        
        given(userRepository.findById(userId)).willReturn(Optional.of(premiumUser));

        // when & then
        assertThatCode(() -> permissionService.validateSubscriptionLimit(userId, SubscriptionPlan.BASIC))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("구독 플랜이 요구사항에 미치지 못하면 예외가 발생한다")
    void validateSubscriptionLimit_WithInsufficientPlan_ShouldThrowException() {
        // given
        Long userId = 3L; // standardUser의 ID
        
        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser)); // BASIC 플랜

        // when & then
        assertThatThrownBy(() -> permissionService.validateSubscriptionLimit(userId, SubscriptionPlan.PRO))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("프로 플랜 플랜 이상이 필요합니다");
    }

    @Test
    @DisplayName("프로젝트 생성 제한을 검증할 수 있다")
    void validateProjectCreationLimit_WithinLimit_ShouldPass() {
        // given
        Long userId = 1L;
        
        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser)); // BASIC 플랜 (1개 제한)
        given(projectRepository.countByUserAndStatus(standardUser, ProjectStatus.ACTIVE)).willReturn(0L);

        // when & then
        assertThatCode(() -> permissionService.validateProjectCreationLimit(userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("프로젝트 생성 제한을 초과하면 예외가 발생한다")
    void validateProjectCreationLimit_ExceedsLimit_ShouldThrowException() {
        // given
        Long userId = 1L;
        
        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser)); // BASIC 플랜 (1개 제한)
        given(projectRepository.countByUserAndStatus(standardUser, ProjectStatus.ACTIVE)).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> permissionService.validateProjectCreationLimit(userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("최대 1개의 프로젝트만 생성할 수 있습니다");
    }

    @Test
    @DisplayName("프로젝트 접근 권한을 강제할 수 있다")
    void enforceProjectAccess_WithValidAccess_ShouldPass() {
        // given
        Long userId = 1L;
        Long projectId = 1L;

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));
        given(projectRepository.findById(projectId)).willReturn(Optional.of(testProject));

        // when & then
        assertThatCode(() -> permissionService.enforceProjectAccess(userId, projectId, "READ"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("프로젝트 접근 권한이 없으면 예외가 발생한다")
    void enforceProjectAccess_WithInvalidAccess_ShouldThrowException() {
        // given
        Long userId = 1L;
        Long projectId = 1L;

        given(userRepository.findById(userId)).willReturn(Optional.of(basicUser));
        given(projectRepository.findById(projectId)).willReturn(Optional.of(testProject));

        // when & then
        assertThatThrownBy(() -> permissionService.enforceProjectAccess(userId, projectId, "DELETE"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("DELETE 권한이 없습니다");
    }

    @Test
    @DisplayName("허용된 리소스 목록을 조회할 수 있다")
    void getAllowedResources_ShouldReturnCorrectList() {
        // given
        Long userId = 1L;
        List<Resource> expectedResources = Arrays.asList(Resource.PROJECT, Resource.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));
        given(rolePolicyRepository.findAllowedResourcesByRoleAndPlan(Role.C, SubscriptionPlan.BASIC))
                .willReturn(expectedResources);

        // when
        List<Resource> result = permissionService.getAllowedResources(userId);

        // then
        assertThat(result).containsExactlyElementsOf(expectedResources);
    }

    @Test
    @DisplayName("허용된 액션 목록을 조회할 수 있다")
    void getAllowedActions_ShouldReturnCorrectList() {
        // given
        Long userId = 1L;
        String resource = "PROJECT";
        List<Action> expectedActions = Arrays.asList(Action.READ, Action.CREATE);

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));
        given(rolePolicyRepository.findAllowedActionsByRoleAndPlanAndResource(
                Role.C, SubscriptionPlan.BASIC, Resource.PROJECT))
                .willReturn(expectedActions);

        // when
        List<Action> result = permissionService.getAllowedActions(userId, resource);

        // then
        assertThat(result).containsExactlyElementsOf(expectedActions);
    }

    @Test
    @DisplayName("관리자는 역할 정책을 업데이트할 수 있다")
    void updateRolePolicy_WithAdmin_ShouldSucceed() {
        // given
        Long adminId = 1L;
        RolePolicy existingPolicy = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);

        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser));
        
        // 관리자 권한 확인을 위한 stub
        given(rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                Role.A, Resource.ROLE_POLICY, Action.MANAGE, SubscriptionPlan.PRO))
                .willReturn(Optional.of(policyFactory.createAllowedPolicy(Role.A, Resource.ROLE_POLICY, Action.MANAGE, SubscriptionPlan.PRO)));
        
        // 실제 정책 조회를 위한 stub
        given(rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC))
                .willReturn(Optional.of(existingPolicy));
        given(rolePolicyRepository.save(existingPolicy)).willReturn(existingPolicy);

        // when & then
        assertThatCode(() -> permissionService.updateRolePolicy(adminId, Role.C, "PROJECT", "CREATE", 
                false, "테스트 이유", SubscriptionPlan.BASIC))
                .doesNotThrowAnyException();

        verify(rolePolicyRepository).save(existingPolicy);
    }

    @Test
    @DisplayName("관리자가 아닌 사용자가 역할 정책을 업데이트하려고 하면 예외가 발생한다")
    void updateRolePolicy_WithNonAdmin_ShouldThrowException() {
        // given
        Long userId = 3L; // standardUser의 ID

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));

        // when & then
        assertThatThrownBy(() -> permissionService.updateRolePolicy(userId, Role.C, "PROJECT", "CREATE", 
                false, "테스트 이유", SubscriptionPlan.BASIC))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ROLE_POLICY 리소스에 대한 MANAGE 권한이 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 권한을 확인하면 예외가 발생한다")
    void hasPermission_WithNonExistentUser_ShouldThrowException() {
        // given
        Long nonExistentUserId = 999L;

        given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> permissionService.hasPermission(nonExistentUserId, "PROJECT", "READ"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트로 접근을 확인하면 예외가 발생한다")
    void checkProjectAccess_WithNonExistentProject_ShouldThrowException() {
        // given
        Long userId = 1L;
        Long nonExistentProjectId = 999L;

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));
        given(projectRepository.findById(nonExistentProjectId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> permissionService.checkProjectAccess(userId, nonExistentProjectId, "READ"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("프로젝트를 찾을 수 없습니다");
    }
}