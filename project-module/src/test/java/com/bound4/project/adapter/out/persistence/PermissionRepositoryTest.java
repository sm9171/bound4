package com.bound4.project.adapter.out.persistence;

import com.bound4.project.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(com.bound4.project.config.TestJpaConfig.class)
@DisplayName("PermissionRepository 통합 테스트")
class PermissionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PermissionRepository permissionRepository;

    private Permission basicProjectCreate;
    private Permission basicProjectRead;
    private Permission proProjectDelete;

    @BeforeEach
    void setUp() {
        basicProjectCreate = new Permission(Resource.PROJECT, Action.CREATE, Role.C, SubscriptionPlan.BASIC, true);
        basicProjectRead = new Permission(Resource.PROJECT, Action.READ, Role.C, SubscriptionPlan.BASIC, true);
        proProjectDelete = new Permission(Resource.PROJECT, Action.DELETE, Role.B, SubscriptionPlan.PRO, true);
        
        entityManager.persistAndFlush(basicProjectCreate);
        entityManager.persistAndFlush(basicProjectRead);
        entityManager.persistAndFlush(proProjectDelete);
        entityManager.clear();
    }

    @Test
    @DisplayName("리소스, 액션, 역할, 구독플랜으로 권한을 조회할 수 있다")
    void findByResourceAndActionAndRoleAndSubscriptionPlan_WithExistingPermission_ShouldReturnPermission() {
        // when
        Optional<Permission> result = permissionRepository.findByResourceAndActionAndRoleAndSubscriptionPlan(
                Resource.PROJECT, Action.CREATE, Role.C, SubscriptionPlan.BASIC);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getResource()).isEqualTo(Resource.PROJECT);
        assertThat(result.get().getAction()).isEqualTo(Action.CREATE);
        assertThat(result.get().getRole()).isEqualTo(Role.C);
        assertThat(result.get().getSubscriptionPlan()).isEqualTo(SubscriptionPlan.BASIC);
        assertThat(result.get().isAllowed()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 권한 조합으로 조회하면 빈 결과를 반환한다")
    void findByResourceAndActionAndRoleAndSubscriptionPlan_WithNonExistentPermission_ShouldReturnEmpty() {
        // when
        Optional<Permission> result = permissionRepository.findByResourceAndActionAndRoleAndSubscriptionPlan(
                Resource.USER, Action.MANAGE, Role.D, SubscriptionPlan.BASIC);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 리소스의 모든 권한을 조회할 수 있다")
    void findByResource_ShouldReturnAllPermissionsForResource() {
        // when
        List<Permission> permissions = permissionRepository.findByResource(Resource.PROJECT);

        // then
        assertThat(permissions).hasSize(3);
        assertThat(permissions).extracting(Permission::getResource).containsOnly(Resource.PROJECT);
        assertThat(permissions).extracting(Permission::getAction)
                .containsExactlyInAnyOrder(Action.CREATE, Action.READ, Action.DELETE);
    }

    @Test
    @DisplayName("특정 역할의 모든 권한을 조회할 수 있다")
    void findByRole_ShouldReturnAllPermissionsForRole() {
        // when
        List<Permission> roleCPermissions = permissionRepository.findByRole(Role.C);
        List<Permission> roleBPermissions = permissionRepository.findByRole(Role.B);

        // then
        assertThat(roleCPermissions).hasSize(2);
        assertThat(roleCPermissions).extracting(Permission::getRole).containsOnly(Role.C);
        
        assertThat(roleBPermissions).hasSize(1);
        assertThat(roleBPermissions).extracting(Permission::getRole).containsOnly(Role.B);
    }

    @Test
    @DisplayName("특정 구독플랜의 모든 권한을 조회할 수 있다")
    void findBySubscriptionPlan_ShouldReturnAllPermissionsForPlan() {
        // when
        List<Permission> basicPermissions = permissionRepository.findBySubscriptionPlan(SubscriptionPlan.BASIC);
        List<Permission> proPermissions = permissionRepository.findBySubscriptionPlan(SubscriptionPlan.PRO);

        // then
        assertThat(basicPermissions).hasSize(2);
        assertThat(basicPermissions).extracting(Permission::getSubscriptionPlan)
                .containsOnly(SubscriptionPlan.BASIC);
        
        assertThat(proPermissions).hasSize(1);
        assertThat(proPermissions).extracting(Permission::getSubscriptionPlan)
                .containsOnly(SubscriptionPlan.PRO);
    }

    @Test
    @DisplayName("허용된 권한만 조회할 수 있다")
    void findByResourceAndAllowed_ShouldReturnCorrectPermissions() {
        // given
        Permission deniedPermission = new Permission(Resource.USER, Action.DELETE, Role.D, SubscriptionPlan.BASIC, false);
        entityManager.persistAndFlush(deniedPermission);

        // when
        List<Permission> allowedProjectPermissions = permissionRepository.findByResourceAndAllowed(Resource.PROJECT, true);
        List<Permission> deniedUserPermissions = permissionRepository.findByResourceAndAllowed(Resource.USER, false);

        // then
        assertThat(allowedProjectPermissions).hasSize(3);
        assertThat(allowedProjectPermissions).extracting(Permission::getResource).containsOnly(Resource.PROJECT);
        assertThat(allowedProjectPermissions).extracting(Permission::isAllowed).containsOnly(true);
        
        assertThat(deniedUserPermissions).hasSize(1);
        assertThat(deniedUserPermissions).extracting(Permission::getResource).containsOnly(Resource.USER);
        assertThat(deniedUserPermissions).extracting(Permission::isAllowed).containsOnly(false);
    }

    @Test
    @DisplayName("리소스와 액션으로 권한을 조회할 수 있다")
    void findByResourceAndAction_ShouldReturnMatchingPermissions() {
        // when
        List<Permission> projectCreatePermissions = permissionRepository.findByResourceAndAction(
                Resource.PROJECT, Action.CREATE);

        // then
        assertThat(projectCreatePermissions).hasSize(1);
        assertThat(projectCreatePermissions.get(0).getResource()).isEqualTo(Resource.PROJECT);
        assertThat(projectCreatePermissions.get(0).getAction()).isEqualTo(Action.CREATE);
    }

    @Test
    @DisplayName("역할과 구독플랜으로 권한을 조회할 수 있다")
    void findByRoleAndSubscriptionPlan_ShouldReturnMatchingPermissions() {
        // when
        List<Permission> roleCBasicPermissions = permissionRepository.findByRoleAndSubscriptionPlan(
                Role.C, SubscriptionPlan.BASIC);

        // then
        assertThat(roleCBasicPermissions).hasSize(2);
        assertThat(roleCBasicPermissions).extracting(Permission::getRole).containsOnly(Role.C);
        assertThat(roleCBasicPermissions).extracting(Permission::getSubscriptionPlan)
                .containsOnly(SubscriptionPlan.BASIC);
    }


    @Test
    @DisplayName("권한을 업데이트할 수 있다")
    void updatePermission_ShouldPersistChanges() {
        // given
        Permission permission = permissionRepository.findById(basicProjectCreate.getId()).orElseThrow();

        // when
        permission.deny();
        Permission savedPermission = permissionRepository.save(permission);
        entityManager.flush();
        entityManager.clear();

        // then
        Permission updatedPermission = permissionRepository.findById(savedPermission.getId()).orElseThrow();
        assertThat(updatedPermission.isAllowed()).isFalse();
    }

    @Test
    @DisplayName("권한을 삭제할 수 있다")
    void deletePermission_ShouldRemoveFromDatabase() {
        // given
        Long permissionId = basicProjectCreate.getId();

        // when
        permissionRepository.deleteById(permissionId);
        entityManager.flush();

        // then
        Optional<Permission> deletedPermission = permissionRepository.findById(permissionId);
        assertThat(deletedPermission).isEmpty();
    }



    @Test
    @DisplayName("특정 액션의 모든 권한을 조회할 수 있다")
    void findByAction_ShouldReturnAllPermissionsForAction() {
        // when
        List<Permission> createPermissions = permissionRepository.findByAction(Action.CREATE);
        List<Permission> deletePermissions = permissionRepository.findByAction(Action.DELETE);

        // then
        assertThat(createPermissions).hasSize(1);
        assertThat(createPermissions.get(0).getAction()).isEqualTo(Action.CREATE);
        
        assertThat(deletePermissions).hasSize(1);
        assertThat(deletePermissions.get(0).getAction()).isEqualTo(Action.DELETE);
    }


    @Test
    @DisplayName("특정 권한이 존재하는지 확인할 수 있다")
    void existsByResourceAndActionAndRoleAndSubscriptionPlan_ShouldReturnCorrectResult() {
        // when
        boolean exists = permissionRepository.existsByResourceAndActionAndRoleAndSubscriptionPlan(
                Resource.PROJECT, Action.CREATE, Role.C, SubscriptionPlan.BASIC);
        boolean notExists = permissionRepository.existsByResourceAndActionAndRoleAndSubscriptionPlan(
                Resource.USER, Action.MANAGE, Role.D, SubscriptionPlan.BASIC);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}