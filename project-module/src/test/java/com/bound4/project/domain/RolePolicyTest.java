package com.bound4.project.domain;

import com.bound4.project.config.TestDataConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RolePolicy 도메인 테스트")
class RolePolicyTest {

    private TestDataConfig.TestRolePolicyFactory policyFactory;

    @BeforeEach
    void setUp() {
        policyFactory = new TestDataConfig.TestRolePolicyFactory();
    }

    @Test
    @DisplayName("유효한 정보로 역할 정책을 생성할 수 있다")
    void createRolePolicy_WithValidInfo_ShouldSucceed() {
        // given
        Role role = Role.C;
        Resource resource = Resource.PROJECT;
        Action action = Action.CREATE;
        boolean allowed = true;
        SubscriptionPlan plan = SubscriptionPlan.BASIC;

        // when
        RolePolicy policy = new RolePolicy(role, resource, action, allowed, plan);

        // then
        assertThat(policy.getRole()).isEqualTo(role);
        assertThat(policy.getResource()).isEqualTo(resource);
        assertThat(policy.getAction()).isEqualTo(action);
        assertThat(policy.isAllowed()).isEqualTo(allowed);
        assertThat(policy.getSubscriptionPlan()).isEqualTo(plan);
        assertThat(policy.isSystemPolicy()).isFalse();
    }

    @Test
    @DisplayName("시스템 정책을 생성할 수 있다")
    void createSystemPolicy_ShouldSucceed() {
        // given
        Role role = Role.A;
        Resource resource = Resource.SYSTEM;
        Action action = Action.MANAGE;
        boolean allowed = true;
        SubscriptionPlan plan = SubscriptionPlan.PRO;

        // when
        RolePolicy systemPolicy = RolePolicy.createSystemPolicy(role, resource, action, allowed, plan);

        // then
        assertThat(systemPolicy.getRole()).isEqualTo(role);
        assertThat(systemPolicy.getResource()).isEqualTo(resource);
        assertThat(systemPolicy.getAction()).isEqualTo(action);
        assertThat(systemPolicy.isAllowed()).isEqualTo(allowed);
        assertThat(systemPolicy.getSubscriptionPlan()).isEqualTo(plan);
        assertThat(systemPolicy.isSystemPolicy()).isTrue();
    }

    @Test
    @DisplayName("커스텀 정책의 권한을 변경할 수 있다")
    void updatePermission_WithCustomPolicy_ShouldUpdate() {
        // given
        RolePolicy policy = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);
        String reason = "정책 변경 테스트";

        // when
        policy.updatePermission(false, reason);

        // then
        assertThat(policy.isAllowed()).isFalse();
        assertThat(policy.getReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("시스템 정책의 권한을 변경하려고 하면 예외가 발생한다")
    void updatePermission_WithSystemPolicy_ShouldThrowException() {
        // given
        RolePolicy systemPolicy = policyFactory.createSystemPolicy(Role.A, Resource.SYSTEM, Action.MANAGE, true, SubscriptionPlan.PRO);
        String reason = "시스템 정책 변경 시도";

        // when & then
        assertThatThrownBy(() -> systemPolicy.updatePermission(false, reason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("시스템 정책은 수정할 수 없습니다");
    }

    @Test
    @DisplayName("커스텀 정책의 이유를 업데이트할 수 있다")
    void updateReason_WithCustomPolicy_ShouldUpdate() {
        // given
        RolePolicy policy = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);
        String newReason = "새로운 이유";

        // when
        policy.updateReason(newReason);

        // then
        assertThat(policy.getReason()).isEqualTo(newReason);
    }

    @Test
    @DisplayName("시스템 정책의 이유를 업데이트하려고 하면 예외가 발생한다")
    void updateReason_WithSystemPolicy_ShouldThrowException() {
        // given
        RolePolicy systemPolicy = policyFactory.createSystemPolicy(Role.A, Resource.SYSTEM, Action.MANAGE, true, SubscriptionPlan.PRO);
        String newReason = "새로운 이유";

        // when & then
        assertThatThrownBy(() -> systemPolicy.updateReason(newReason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("시스템 정책은 수정할 수 없습니다");
    }

    @Test
    @DisplayName("사용자에게 적용 가능한 정책인지 확인할 수 있다")
    void isApplicableFor_ShouldReturnCorrectResult() {
        // given
        RolePolicy policy = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);
        User matchingUser = new User("user@test.com", "password123!", Role.C, SubscriptionPlan.BASIC);
        User nonMatchingUser = new User("admin@test.com", "password123!", Role.A, SubscriptionPlan.PRO);

        // when & then
        assertThat(policy.isApplicableFor(matchingUser, Resource.PROJECT, Action.CREATE)).isTrue();
        assertThat(policy.isApplicableFor(nonMatchingUser, Resource.PROJECT, Action.CREATE)).isFalse();
    }

    @Test
    @DisplayName("정책 매칭이 올바르게 작동한다")
    void matches_ShouldReturnCorrectResult() {
        // given
        RolePolicy policy = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);

        // when & then
        assertThat(policy.matches(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC)).isTrue();
        assertThat(policy.matches(Role.A, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC)).isFalse();
        assertThat(policy.matches(Role.C, Resource.USER, Action.CREATE, SubscriptionPlan.BASIC)).isFalse();
        assertThat(policy.matches(Role.C, Resource.PROJECT, Action.DELETE, SubscriptionPlan.BASIC)).isFalse();
        assertThat(policy.matches(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.PRO)).isFalse();
    }

    @Test
    @DisplayName("커스텀 정책은 수정 가능하다")
    void canBeModified_WithCustomPolicy_ShouldReturnTrue() {
        // given
        RolePolicy customPolicy = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);

        // when & then
        assertThat(customPolicy.canBeModified()).isTrue();
    }

    @Test
    @DisplayName("시스템 정책은 수정 불가능하다")
    void canBeModified_WithSystemPolicy_ShouldReturnFalse() {
        // given
        RolePolicy systemPolicy = policyFactory.createSystemPolicy(Role.A, Resource.SYSTEM, Action.MANAGE, true, SubscriptionPlan.PRO);

        // when & then
        assertThat(systemPolicy.canBeModified()).isFalse();
    }

    @Test
    @DisplayName("같은 속성을 가진 정책들은 동일하다")
    void equals_WithSameAttributes_ShouldReturnTrue() {
        // given
        RolePolicy policy1 = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);
        RolePolicy policy2 = policyFactory.createDeniedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);

        // when & then
        assertThat(policy1).isEqualTo(policy2); // allowed 값이 달라도 같은 정책으로 간주
        assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode());
    }

    @Test
    @DisplayName("다른 속성을 가진 정책들은 동일하지 않다")
    void equals_WithDifferentAttributes_ShouldReturnFalse() {
        // given
        RolePolicy policy1 = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.CREATE, SubscriptionPlan.BASIC);
        RolePolicy policy2 = policyFactory.createAllowedPolicy(Role.C, Resource.PROJECT, Action.DELETE, SubscriptionPlan.BASIC);

        // when & then
        assertThat(policy1).isNotEqualTo(policy2);
    }
}