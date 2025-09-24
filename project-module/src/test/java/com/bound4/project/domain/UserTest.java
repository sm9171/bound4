package com.bound4.project.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 도메인 테스트")
class UserTest {

    @Test
    @DisplayName("유효한 정보로 사용자를 생성할 수 있다")
    void createUser_WithValidInfo_ShouldSucceed() {
        // given
        String email = "test@example.com";
        String password = "password123!";
        Role role = Role.C;
        SubscriptionPlan plan = SubscriptionPlan.BASIC;

        // when
        User user = new User(email, password, role, plan);

        // then
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getRole()).isEqualTo(role);
        assertThat(user.getSubscriptionPlan()).isEqualTo(plan);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid-email"})
    @DisplayName("잘못된 이메일 형식으로 사용자를 생성하면 예외가 발생한다")
    void createUser_WithInvalidEmail_ShouldThrowException(String invalidEmail) {
        // given
        String password = "password123!";
        Role role = Role.C;
        SubscriptionPlan plan = SubscriptionPlan.BASIC;

        // when & then
        assertThatThrownBy(() -> new User(invalidEmail, password, role, plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");
    }

    @Test
    @DisplayName("빈 비밀번호로 사용자를 생성하면 예외가 발생한다")
    void createUser_WithEmptyPassword_ShouldThrowException() {
        // given
        String email = "test@example.com";
        String emptyPassword = "";
        Role role = Role.C;
        SubscriptionPlan plan = SubscriptionPlan.BASIC;

        // when & then
        assertThatThrownBy(() -> new User(email, emptyPassword, role, plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호는 필수입니다");
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "1234567"})
    @DisplayName("8자 미만의 비밀번호로 사용자를 생성하면 예외가 발생한다")
    void createUser_WithShortPassword_ShouldThrowException(String shortPassword) {
        // given
        String email = "test@example.com";
        Role role = Role.C;
        SubscriptionPlan plan = SubscriptionPlan.BASIC;

        // when & then
        assertThatThrownBy(() -> new User(email, shortPassword, role, plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호는 최소 8자 이상이어야 합니다");
    }

    @Test
    @DisplayName("사용자 역할을 변경할 수 있다")
    void changeRole_ShouldUpdateRole() {
        // given
        User user = new User("test@example.com", "password123!", Role.C, SubscriptionPlan.BASIC);
        Role newRole = Role.B;

        // when
        user.changeRole(newRole);

        // then
        assertThat(user.getRole()).isEqualTo(newRole);
    }

    @Test
    @DisplayName("구독 플랜을 업그레이드할 수 있다")
    void upgradeSubscriptionPlan_ShouldUpdatePlan() {
        // given
        User user = new User("test@example.com", "password123!", Role.C, SubscriptionPlan.BASIC);
        SubscriptionPlan newPlan = SubscriptionPlan.PRO;

        // when
        user.upgradeSubscriptionPlan(newPlan);

        // then
        assertThat(user.getSubscriptionPlan()).isEqualTo(newPlan);
    }

    @ParameterizedTest
    @CsvSource({
            "A, PROJECT, CREATE, BASIC, true",
            "A, USER, DELETE, PRO, true",
            "B, PROJECT, READ, PRO, true",
            "C, PROJECT, CREATE, BASIC, true",
            "D, PROJECT, READ, BASIC, true",
            "D, PROJECT, DELETE, BASIC, false"
    })
    @DisplayName("역할과 구독 플랜에 따른 권한 검증이 올바르게 작동한다")
    void hasPermission_ShouldReturnCorrectResult(Role role, String resource, String action, 
                                                SubscriptionPlan plan, boolean expected) {
        // given
        User user = new User("test@example.com", "password123!", role, plan);

        // when
        boolean result = user.hasPermission(resource, action);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("비밀번호를 변경할 수 있다")
    void changePassword_WithValidPassword_ShouldUpdate() {
        // given
        User user = new User("test@example.com", "oldPassword123!", Role.C, SubscriptionPlan.BASIC);
        String newPassword = "newPassword123!";

        // when
        user.changePassword(newPassword);

        // then
        assertThat(user.getPassword()).isEqualTo(newPassword);
    }

    @Test
    @DisplayName("짧은 비밀번호로 변경하면 예외가 발생한다")
    void changePassword_WithShortPassword_ShouldThrowException() {
        // given
        User user = new User("test@example.com", "oldPassword123!", Role.C, SubscriptionPlan.BASIC);
        String shortPassword = "short";

        // when & then
        assertThatThrownBy(() -> user.changePassword(shortPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호는 최소 8자 이상이어야 합니다");
    }

    @Test
    @DisplayName("같은 이메일을 가진 사용자는 동일하다")
    void equals_WithSameEmail_ShouldReturnTrue() {
        // given
        String email = "test@example.com";
        User user1 = new User(email, "password1", Role.A, SubscriptionPlan.PRO);
        User user2 = new User(email, "password2", Role.B, SubscriptionPlan.BASIC);

        // when & then
        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("다른 이메일을 가진 사용자는 동일하지 않다")
    void equals_WithDifferentEmail_ShouldReturnFalse() {
        // given
        User user1 = new User("test1@example.com", "password", Role.A, SubscriptionPlan.PRO);
        User user2 = new User("test2@example.com", "password", Role.A, SubscriptionPlan.PRO);

        // when & then
        assertThat(user1).isNotEqualTo(user2);
    }
}