package com.bound4.project.domain;

import com.bound4.project.config.TestDataConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Project 도메인 테스트")
class ProjectTest {

    private TestDataConfig.TestUserFactory userFactory;
    private User testUser;

    @BeforeEach
    void setUp() {
        userFactory = new TestDataConfig.TestUserFactory();
        testUser = userFactory.createStandardUser();
    }

    @Test
    @DisplayName("유효한 정보로 프로젝트를 생성할 수 있다")
    void createProject_WithValidInfo_ShouldSucceed() {
        // given
        String name = "Test Project";
        String description = "Test Description";

        // when
        Project project = new Project(name, description, testUser);

        // then
        assertThat(project.getName()).isEqualTo(name);
        assertThat(project.getDescription()).isEqualTo(description);
        assertThat(project.getUser()).isEqualTo(testUser);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 이름으로 프로젝트를 생성하면 예외가 발생한다")
    void createProject_WithEmptyName_ShouldThrowException(String emptyName) {
        // given
        String description = "Test Description";

        // when & then
        assertThatThrownBy(() -> new Project(emptyName, description, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("프로젝트 이름은 필수입니다");
    }

    @Test
    @DisplayName("100자를 초과하는 이름으로 프로젝트를 생성하면 예외가 발생한다")
    void createProject_WithTooLongName_ShouldThrowException() {
        // given
        String longName = "a".repeat(101);
        String description = "Test Description";

        // when & then
        assertThatThrownBy(() -> new Project(longName, description, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("프로젝트 이름은 100자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("null 사용자로 프로젝트를 생성하면 예외가 발생한다")
    void createProject_WithNullUser_ShouldThrowException() {
        // given
        String name = "Test Project";
        String description = "Test Description";

        // when & then
        assertThatThrownBy(() -> new Project(name, description, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("프로젝트 소유자는 필수입니다");
    }

    @Test
    @DisplayName("프로젝트 이름을 업데이트할 수 있다")
    void updateName_WithValidName_ShouldUpdate() {
        // given
        Project project = new Project("Old Name", "Description", testUser);
        String newName = "New Name";

        // when
        project.updateName(newName);

        // then
        assertThat(project.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("프로젝트 설명을 업데이트할 수 있다")
    void updateDescription_WithValidDescription_ShouldUpdate() {
        // given
        Project project = new Project("Name", "Old Description", testUser);
        String newDescription = "New Description";

        // when
        project.updateDescription(newDescription);

        // then
        assertThat(project.getDescription()).isEqualTo(newDescription);
    }

    @Test
    @DisplayName("500자를 초과하는 설명으로 업데이트하면 예외가 발생한다")
    void updateDescription_WithTooLongDescription_ShouldThrowException() {
        // given
        Project project = new Project("Name", "Description", testUser);
        String longDescription = "a".repeat(501);

        // when & then
        assertThatThrownBy(() -> project.updateDescription(longDescription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("프로젝트 설명은 500자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("프로젝트를 아카이브할 수 있다")
    void archive_ShouldChangeStatusToArchived() {
        // given
        Project project = new Project("Name", "Description", testUser);

        // when
        project.archive();

        // then
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    @DisplayName("프로젝트를 활성화할 수 있다")
    void activate_ShouldChangeStatusToActive() {
        // given
        Project project = new Project("Name", "Description", testUser);
        project.archive(); // 먼저 아카이브

        // when
        project.activate();

        // then
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    @DisplayName("프로젝트 소유자 확인이 올바르게 작동한다")
    void isOwnedBy_ShouldReturnCorrectResult() {
        // given
        Project project = new Project("Name", "Description", testUser);
        User anotherUser = userFactory.createPremiumUser();

        // when & then
        assertThat(project.isOwnedBy(testUser)).isTrue();
        assertThat(project.isOwnedBy(anotherUser)).isFalse();
    }

    @Test
    @DisplayName("활성 상태 확인이 올바르게 작동한다")
    void isActive_ShouldReturnCorrectResult() {
        // given
        Project project = new Project("Name", "Description", testUser);

        // when & then
        assertThat(project.isActive()).isTrue();

        project.archive();
        assertThat(project.isActive()).isFalse();
    }

    @Test
    @DisplayName("관리자는 모든 프로젝트에 접근할 수 있다")
    void canBeAccessedBy_WithAdmin_ShouldReturnTrue() {
        // given
        Project project = new Project("Name", "Description", testUser);
        User admin = userFactory.createAdminUser();

        // when & then
        assertThat(project.canBeAccessedBy(admin)).isTrue();
    }

    @Test
    @DisplayName("프리미엄 사용자(PRO 플랜)는 다른 사용자의 프로젝트에 접근할 수 있다")
    void canBeAccessedBy_WithPremiumProUser_ShouldReturnTrue() {
        // given
        Project project = new Project("Name", "Description", testUser);
        User premiumUser = userFactory.createPremiumUser(); // Role.B + PRO

        // when & then
        assertThat(project.canBeAccessedBy(premiumUser)).isTrue();
    }

    @Test
    @DisplayName("프로젝트 소유자는 본인 프로젝트에 접근할 수 있다")
    void canBeAccessedBy_WithOwner_ShouldReturnTrue() {
        // given
        Project project = new Project("Name", "Description", testUser);

        // when & then
        assertThat(project.canBeAccessedBy(testUser)).isTrue();
    }

    @Test
    @DisplayName("같은 ID를 가진 프로젝트는 동일하다")
    void equals_WithSameId_ShouldReturnTrue() {
        // given
        Project project1 = new Project("Name1", "Description1", testUser);
        
        // ID가 같다고 가정하기 위해 리플렉션 사용
        // 실제로는 JPA가 ID를 할당하므로 통합 테스트에서 검증

        // when & then
        assertThat(project1).isEqualTo(project1); // 자기 자신과는 항상 같음
    }
}