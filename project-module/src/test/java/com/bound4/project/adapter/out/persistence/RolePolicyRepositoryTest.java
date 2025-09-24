package com.bound4.project.adapter.out.persistence;

import com.bound4.project.config.TestDataConfig;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(com.bound4.project.config.TestJpaConfig.class)
@DisplayName("RolePolicyRepository 통합 테스트")
class RolePolicyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RolePolicyRepository rolePolicyRepository;

    private TestDataConfig.TestRolePolicyFactory policyFactory;
    private RolePolicy customPolicy1;
    private RolePolicy customPolicy2;
    private RolePolicy systemPolicy;

    @BeforeEach
    void setUp() {
        policyFactory = new TestDataConfig.TestRolePolicyFactory();
        
        // 커스텀 정책들
        customPolicy1 = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);
        customPolicy2 = policyFactory.createDeniedPolicy(Role.C, Resource.PROJECT, Action.DELETE, SubscriptionPlan.BASIC);
        
        // 시스템 정책
        systemPolicy = policyFactory.createSystemPolicy(Role.A, Resource.SYSTEM, Action.MANAGE, true, SubscriptionPlan.PRO);
        
        entityManager.persistAndFlush(customPolicy1);
        entityManager.persistAndFlush(customPolicy2);
        entityManager.persistAndFlush(systemPolicy);
        entityManager.clear();
    }

    @Test
    @DisplayName("역할, 리소스, 액션, 구독플랜으로 정책을 조회할 수 있다")
    void findByRoleAndResourceAndActionAndSubscriptionPlan_WithExistingPolicy_ShouldReturnPolicy() {
        // when
        Optional<RolePolicy> result = rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(Role.C);
        assertThat(result.get().getResource()).isEqualTo(Resource.PROJECT);
        assertThat(result.get().getAction()).isEqualTo(Action.CREATE);
        assertThat(result.get().isAllowed()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 정책 조합으로 조회하면 빈 결과를 반환한다")
    void findByRoleAndResourceAndActionAndSubscriptionPlan_WithNonExistentPolicy_ShouldReturnEmpty() {
        // when
        Optional<RolePolicy> result = rolePolicyRepository.findByRoleAndResourceAndActionAndSubscriptionPlan(
                Role.D, Resource.USER, Action.MANAGE, SubscriptionPlan.PRO);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 역할의 모든 정책을 조회할 수 있다")
    void findByRole_ShouldReturnAllPoliciesForRole() {
        // when
        List<RolePolicy> policies = rolePolicyRepository.findByRole(Role.C);

        // then
        assertThat(policies).hasSize(2);
        assertThat(policies).extracting(RolePolicy::getRole).containsOnly(Role.C);
        assertThat(policies).extracting(RolePolicy::getAction)
                .containsExactlyInAnyOrder(Action.CREATE, Action.DELETE);
    }

    @Test
    @DisplayName("특정 리소스의 모든 정책을 조회할 수 있다")
    void findByResource_ShouldReturnAllPoliciesForResource() {
        // when
        List<RolePolicy> policies = rolePolicyRepository.findByResource(Resource.PROJECT);

        // then
        assertThat(policies).hasSize(2);
        assertThat(policies).extracting(RolePolicy::getResource).containsOnly(Resource.PROJECT);
    }

    @Test
    @DisplayName("허용된 정책만 조회할 수 있다")
    void findAllowedPolicies_ShouldReturnOnlyAllowedPolicies() {
        // when
        List<RolePolicy> allowedPolicies = rolePolicyRepository.findAllowedPolicies(Role.C, SubscriptionPlan.BASIC);

        // then
        assertThat(allowedPolicies).hasSize(1); // customPolicy1
        assertThat(allowedPolicies).extracting(RolePolicy::isAllowed).containsOnly(true);
    }

    @Test
    @DisplayName("거부된 정책만 조회할 수 있다")
    void findDeniedPolicies_ShouldReturnOnlyDeniedPolicies() {
        // when
        List<RolePolicy> deniedPolicies = rolePolicyRepository.findDeniedPolicies(Role.C, SubscriptionPlan.BASIC);

        // then
        assertThat(deniedPolicies).hasSize(1); // customPolicy2
        assertThat(deniedPolicies).extracting(RolePolicy::isAllowed).containsOnly(false);
    }

    @Test
    @DisplayName("커스텀 정책만 조회할 수 있다")
    void findCustomPolicies_ShouldReturnOnlyCustomPolicies() {
        // when
        List<RolePolicy> customPolicies = rolePolicyRepository.findCustomPolicies();

        // then
        assertThat(customPolicies).hasSize(2);
        assertThat(customPolicies).extracting(RolePolicy::isSystemPolicy).containsOnly(false);
    }

    @Test
    @DisplayName("시스템 정책만 조회할 수 있다")
    void findSystemPolicies_ShouldReturnOnlySystemPolicies() {
        // when
        List<RolePolicy> systemPolicies = rolePolicyRepository.findSystemPolicies();

        // then
        assertThat(systemPolicies).hasSize(1);
        assertThat(systemPolicies).extracting(RolePolicy::isSystemPolicy).containsOnly(true);
    }

    @Test
    @DisplayName("역할과 구독플랜으로 허용된 리소스 목록을 조회할 수 있다")
    void findAllowedResourcesByRoleAndPlan_ShouldReturnAllowedResources() {
        // when
        List<Resource> allowedResources = rolePolicyRepository.findAllowedResourcesByRoleAndPlan(
                Role.C, SubscriptionPlan.BASIC);

        // then
        assertThat(allowedResources).hasSize(1);
        assertThat(allowedResources).contains(Resource.PROJECT);
    }

    @Test
    @DisplayName("역할, 구독플랜, 리소스로 허용된 액션 목록을 조회할 수 있다")
    void findAllowedActionsByRoleAndPlanAndResource_ShouldReturnAllowedActions() {
        // when
        List<Action> allowedActions = rolePolicyRepository.findAllowedActionsByRoleAndPlanAndResource(
                Role.C, SubscriptionPlan.BASIC, Resource.PROJECT);

        // then
        assertThat(allowedActions).hasSize(1);
        assertThat(allowedActions).contains(Action.CREATE);
    }

    @Test
    @DisplayName("역할과 구독플랜으로 정책을 조회할 수 있다")
    void findByRoleAndSubscriptionPlan_ShouldReturnPoliciesForRoleAndPlan() {
        // when
        List<RolePolicy> basicPolicies = rolePolicyRepository.findByRoleAndSubscriptionPlan(Role.C, SubscriptionPlan.BASIC);
        List<RolePolicy> proPolicies = rolePolicyRepository.findByRoleAndSubscriptionPlan(Role.A, SubscriptionPlan.PRO);

        // then
        assertThat(basicPolicies).hasSize(2);
        assertThat(basicPolicies).extracting(RolePolicy::getRole).containsOnly(Role.C);
        assertThat(basicPolicies).extracting(RolePolicy::getSubscriptionPlan)
                .containsOnly(SubscriptionPlan.BASIC);
        
        assertThat(proPolicies).hasSize(1);
        assertThat(proPolicies).extracting(RolePolicy::getRole).containsOnly(Role.A);
        assertThat(proPolicies).extracting(RolePolicy::getSubscriptionPlan)
                .containsOnly(SubscriptionPlan.PRO);
    }

    @Test
    @DisplayName("정책을 업데이트할 수 있다")
    void updatePolicy_ShouldPersistChanges() {
        // given
        RolePolicy policy = rolePolicyRepository.findById(customPolicy1.getId()).orElseThrow();
        String newReason = "업데이트된 이유";

        // when
        policy.updatePermission(false, newReason);
        RolePolicy savedPolicy = rolePolicyRepository.save(policy);
        entityManager.flush();
        entityManager.clear();

        // then
        RolePolicy updatedPolicy = rolePolicyRepository.findById(savedPolicy.getId()).orElseThrow();
        assertThat(updatedPolicy.isAllowed()).isFalse();
        assertThat(updatedPolicy.getReason()).isEqualTo(newReason);
    }

    @Test
    @DisplayName("정책을 삭제할 수 있다")
    void deletePolicy_ShouldRemoveFromDatabase() {
        // given
        Long policyId = customPolicy1.getId();

        // when
        rolePolicyRepository.deleteById(policyId);
        entityManager.flush();

        // then
        Optional<RolePolicy> deletedPolicy = rolePolicyRepository.findById(policyId);
        assertThat(deletedPolicy).isEmpty();
    }

    @Test
    @DisplayName("역할과 리소스로 허용된 정책을 검색할 수 있다")
    void findAllowedPoliciesByRoleAndResource_ShouldReturnMatchingPolicies() {
        // when
        List<RolePolicy> allowedProjectPolicies = rolePolicyRepository.findAllowedPoliciesByRoleAndResource(
                Role.C, Resource.PROJECT);

        // then
        assertThat(allowedProjectPolicies).hasSize(1);
        assertThat(allowedProjectPolicies.get(0).getAction()).isEqualTo(Action.CREATE);
        assertThat(allowedProjectPolicies.get(0).isAllowed()).isTrue();
    }

    @Test
    @DisplayName("커스텀 정책 개수를 조회할 수 있다")
    void countCustomPolicies_ShouldReturnCorrectCount() {
        // when
        long customPolicyCount = rolePolicyRepository.countCustomPolicies();

        // then
        assertThat(customPolicyCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("역할별 허용된 정책 개수를 조회할 수 있다")
    void countAllowedPoliciesByRole_ShouldReturnCorrectCount() {
        // when
        long roleCCount = rolePolicyRepository.countAllowedPoliciesByRole(Role.C);
        long roleACount = rolePolicyRepository.countAllowedPoliciesByRole(Role.A);

        // then
        assertThat(roleCCount).isEqualTo(1L); // customPolicy1만 허용됨
        assertThat(roleACount).isEqualTo(1L); // systemPolicy
    }

    @Test
    @DisplayName("시스템 정책과 커스텀 정책을 구분하여 조회할 수 있다")
    void findBySystemPolicy_ShouldReturnCorrectPolicies() {
        // when
        List<RolePolicy> systemPolicies = rolePolicyRepository.findBySystemPolicy(true);
        List<RolePolicy> customPolicies = rolePolicyRepository.findBySystemPolicy(false);

        // then
        assertThat(systemPolicies).hasSize(1);
        assertThat(systemPolicies.get(0).isSystemPolicy()).isTrue();
        
        assertThat(customPolicies).hasSize(2);
        assertThat(customPolicies).extracting(RolePolicy::isSystemPolicy).containsOnly(false);
    }
}