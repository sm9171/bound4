package com.bound4.project.adapter.out.persistence;

import com.bound4.project.config.TestDataConfig;
import com.bound4.project.domain.Role;
import com.bound4.project.domain.SubscriptionPlan;
import com.bound4.project.domain.User;
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
@DisplayName("UserRepository 통합 테스트")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private TestDataConfig.TestUserFactory userFactory;
    private User adminUser;
    private User premiumUser;
    private User standardUser;
    private User basicUser;

    @BeforeEach
    void setUp() {
        userFactory = new TestDataConfig.TestUserFactory();
        
        adminUser = userFactory.createAdminUser();
        premiumUser = userFactory.createPremiumUser();
        standardUser = userFactory.createStandardUser();
        basicUser = userFactory.createBasicUser();
        
        entityManager.persistAndFlush(adminUser);
        entityManager.persistAndFlush(premiumUser);
        entityManager.persistAndFlush(standardUser);
        entityManager.persistAndFlush(basicUser);
        entityManager.clear();
    }

    @Test
    @DisplayName("이메일로 사용자를 조회할 수 있다")
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        // when
        Optional<User> result = userRepository.findByEmail(adminUser.getEmail());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(adminUser.getEmail());
        assertThat(result.get().getRole()).isEqualTo(Role.A);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 빈 결과를 반환한다")
    void findByEmail_WithNonExistentEmail_ShouldReturnEmpty() {
        // when
        Optional<User> result = userRepository.findByEmail("nonexistent@test.com");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("역할과 구독 플랜으로 사용자 목록을 조회할 수 있다")
    void findByRoleAndSubscriptionPlan_ShouldReturnMatchingUsers() {
        // when
        List<User> proUsers = userRepository.findByRoleAndSubscriptionPlan(Role.B, SubscriptionPlan.PRO);
        List<User> basicUsers = userRepository.findByRoleAndSubscriptionPlan(Role.C, SubscriptionPlan.BASIC);

        // then
        assertThat(proUsers).hasSize(1);
        assertThat(proUsers.get(0).getRole()).isEqualTo(Role.B);
        assertThat(proUsers.get(0).getSubscriptionPlan()).isEqualTo(SubscriptionPlan.PRO);
        
        assertThat(basicUsers).hasSize(1);
        assertThat(basicUsers.get(0).getRole()).isEqualTo(Role.C);
        assertThat(basicUsers.get(0).getSubscriptionPlan()).isEqualTo(SubscriptionPlan.BASIC);
    }

    @Test
    @DisplayName("특정 역할의 사용자 목록을 조회할 수 있다")
    void findByRole_ShouldReturnUsersWithRole() {
        // when
        List<User> result = userRepository.findByRole(Role.C);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(Role.C);
    }

    @Test
    @DisplayName("특정 구독 플랜의 사용자 개수를 조회할 수 있다")
    void countBySubscriptionPlan_ShouldReturnCorrectCount() {
        // when
        long basicCount = userRepository.countBySubscriptionPlan(SubscriptionPlan.BASIC);
        long proCount = userRepository.countBySubscriptionPlan(SubscriptionPlan.PRO);

        // then
        assertThat(basicCount).isEqualTo(2L); // standardUser, basicUser
        assertThat(proCount).isEqualTo(2L); // adminUser, premiumUser
    }

    @Test
    @DisplayName("이메일 패턴으로 사용자를 검색할 수 있다")
    void findByEmailContaining_ShouldReturnMatchingUsers() {
        // when
        List<User> adminUsers = userRepository.findByEmailContaining("admin");

        // then
        assertThat(adminUsers).hasSize(1);
        assertThat(adminUsers.get(0).getEmail()).contains("admin");
    }

    @Test
    @DisplayName("이메일 존재 여부를 확인할 수 있다")
    void existsByEmail_WithExistingEmail_ShouldReturnTrue() {
        // when
        boolean exists = userRepository.existsByEmail(adminUser.getEmail());
        boolean notExists = userRepository.existsByEmail("nonexistent@test.com");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("관리자 사용자를 조회할 수 있다")
    void findAdmins_ShouldReturnAdminUsers() {
        // when
        List<User> admins = userRepository.findByRole(Role.A);

        // then
        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getRole()).isEqualTo(Role.A);
        assertThat(admins.get(0).getEmail()).isEqualTo(adminUser.getEmail());
    }

    @Test
    @DisplayName("역할별 사용자 개수를 조회할 수 있다")
    void countByRole_ShouldReturnCorrectCount() {
        // when
        long adminCount = userRepository.countByRole(Role.A);
        long standardCount = userRepository.countByRole(Role.C);

        // then
        assertThat(adminCount).isEqualTo(1L);
        assertThat(standardCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("특정 날짜 이후 생성된 사용자를 조회할 수 있다")
    void findUsersCreatedAfter_ShouldReturnRecentUsers() {
        // given
        java.time.LocalDateTime yesterday = java.time.LocalDateTime.now().minusDays(1);

        // when
        List<User> recentUsers = userRepository.findUsersCreatedAfter(yesterday);

        // then
        assertThat(recentUsers).hasSize(4); // 모든 사용자가 오늘 생성됨
        assertThat(recentUsers).allMatch(user -> user.getCreatedAt().isAfter(yesterday));
    }
}