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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(com.bound4.project.config.TestJpaConfig.class)
@DisplayName("ProjectRepository 통합 테스트")
class ProjectRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectRepository projectRepository;

    private TestDataConfig.TestUserFactory userFactory;
    private TestDataConfig.TestProjectFactory projectFactory;
    
    private User standardUser;
    private User adminUser;
    private Project activeProject1;
    private Project activeProject2;
    private Project archivedProject;

    @BeforeEach
    void setUp() {
        userFactory = new TestDataConfig.TestUserFactory();
        projectFactory = new TestDataConfig.TestProjectFactory();
        
        standardUser = userFactory.createStandardUser();
        adminUser = userFactory.createAdminUser();
        
        entityManager.persistAndFlush(standardUser);
        entityManager.persistAndFlush(adminUser);
        
        activeProject1 = projectFactory.createProject("Active Project 1", "Description 1", standardUser);
        activeProject2 = projectFactory.createProject("Active Project 2", "Description 2", standardUser);
        archivedProject = projectFactory.createProject("Archived Project", "Archived Description", standardUser);
        archivedProject.archive();
        
        entityManager.persistAndFlush(activeProject1);
        entityManager.persistAndFlush(activeProject2);
        entityManager.persistAndFlush(archivedProject);
        entityManager.clear();
    }

    @Test
    @DisplayName("사용자와 프로젝트 정보를 함께 조회할 수 있다")
    void findByIdWithUser_WithExistingProject_ShouldReturnProjectWithUser() {
        // when
        Optional<Project> result = projectRepository.findByIdWithUser(activeProject1.getId());

        // then
        assertThat(result).isPresent();
        Project project = result.get();
        assertThat(project.getName()).isEqualTo("Active Project 1");
        assertThat(project.getUser().getEmail()).isEqualTo(standardUser.getEmail());
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 ID로 조회하면 빈 결과를 반환한다")
    void findByIdWithUser_WithNonExistentId_ShouldReturnEmpty() {
        // when
        Optional<Project> result = projectRepository.findByIdWithUser(999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자별 활성 프로젝트를 페이지네이션으로 조회할 수 있다")
    void findByUserAndStatus_WithActiveStatus_ShouldReturnActiveProjects() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Project> result = projectRepository.findByUserAndStatus(standardUser, ProjectStatus.ACTIVE, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).extracting(Project::getStatus)
                .containsOnly(ProjectStatus.ACTIVE);
        assertThat(result.getContent()).extracting(Project::getName)
                .containsExactlyInAnyOrder("Active Project 1", "Active Project 2");
    }

    @Test
    @DisplayName("전체 활성 프로젝트를 페이지네이션으로 조회할 수 있다")
    void findByStatus_WithActiveStatus_ShouldReturnAllActiveProjects() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Project> result = projectRepository.findByStatus(ProjectStatus.ACTIVE, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).extracting(Project::getStatus)
                .containsOnly(ProjectStatus.ACTIVE);
    }

    @Test
    @DisplayName("사용자와 상태별 프로젝트 개수를 조회할 수 있다")
    void countByUserAndStatus_ShouldReturnCorrectCount() {
        // when
        long activeCount = projectRepository.countByUserAndStatus(standardUser, ProjectStatus.ACTIVE);
        long archivedCount = projectRepository.countByUserAndStatus(standardUser, ProjectStatus.ARCHIVED);

        // then
        assertThat(activeCount).isEqualTo(2L);
        assertThat(archivedCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("사용자의 전체 프로젝트 목록을 조회할 수 있다")
    void findByUser_ShouldReturnAllUserProjects() {
        // when
        List<Project> userProjects = projectRepository.findByUser(standardUser);

        // then
        assertThat(userProjects).hasSize(3); // 활성 2개 + 아카이브 1개
        assertThat(userProjects).extracting(Project::getUser).containsOnly(standardUser);
    }

    @Test
    @DisplayName("사용자와 프로젝트 이름으로 중복 확인을 할 수 있다")
    void existsByUserAndName_WithExistingName_ShouldReturnTrue() {
        // when
        boolean exists = projectRepository.existsByUserAndName(standardUser, "Active Project 1");
        boolean notExists = projectRepository.existsByUserAndName(standardUser, "Non-existent Project");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("특정 기간 이후에 생성된 프로젝트를 조회할 수 있다")
    void findProjectsCreatedAfter_ShouldReturnRecentProjects() {
        // given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // when
        List<Project> recentProjects = projectRepository.findProjectsCreatedAfter(yesterday);

        // then
        assertThat(recentProjects).hasSize(3); // 모든 프로젝트가 오늘 생성됨
    }

    @Test
    @DisplayName("특정 기간 이후에 업데이트된 프로젝트를 조회할 수 있다")
    void findProjectsUpdatedAfter_ShouldReturnRecentProjects() {
        // given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // when
        List<Project> updatedProjects = projectRepository.findProjectsUpdatedAfter(yesterday);

        // then
        assertThat(updatedProjects).hasSize(3); // 모든 프로젝트가 오늘 업데이트됨
    }

    @Test
    @DisplayName("프로젝트 이름으로 부분 검색을 할 수 있다")
    void findByNameContaining_ShouldReturnMatchingProjects() {
        // when
        List<Project> projects = projectRepository.findByNameContaining("Active");

        // then
        assertThat(projects).hasSize(2);
        assertThat(projects).extracting(Project::getName)
                .allMatch(name -> name.contains("Active"));
    }

    @Test
    @DisplayName("사용자 ID로 프로젝트를 조회할 수 있다")
    void findByUserId_ShouldReturnUserProjects() {
        // when
        List<Project> userProjects = projectRepository.findByUserId(standardUser.getId());

        // then
        assertThat(userProjects).hasSize(3);
        assertThat(userProjects).extracting(project -> project.getUser().getId())
                .containsOnly(standardUser.getId());
    }

    @Test
    @DisplayName("프로젝트 정보를 업데이트할 수 있다")
    void updateProject_ShouldPersistChanges() {
        // given
        Project project = projectRepository.findById(activeProject1.getId()).orElseThrow();
        String newName = "Updated Project Name";
        String newDescription = "Updated Description";

        // when
        project.updateName(newName);
        project.updateDescription(newDescription);
        Project savedProject = projectRepository.save(project);
        entityManager.flush();
        entityManager.clear();

        // then
        Project updatedProject = projectRepository.findById(savedProject.getId()).orElseThrow();
        assertThat(updatedProject.getName()).isEqualTo(newName);
        assertThat(updatedProject.getDescription()).isEqualTo(newDescription);
    }

    @Test
    @DisplayName("프로젝트 상태를 변경할 수 있다")
    void updateProjectStatus_ShouldPersistChanges() {
        // given
        Project project = projectRepository.findById(activeProject1.getId()).orElseThrow();

        // when
        project.archive();
        Project savedProject = projectRepository.save(project);
        entityManager.flush();
        entityManager.clear();

        // then
        Project updatedProject = projectRepository.findById(savedProject.getId()).orElseThrow();
        assertThat(updatedProject.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    @DisplayName("프로젝트를 삭제할 수 있다")
    void deleteProject_ShouldRemoveFromDatabase() {
        // given
        Long projectId = activeProject1.getId();

        // when
        projectRepository.deleteById(projectId);
        entityManager.flush();

        // then
        Optional<Project> deletedProject = projectRepository.findById(projectId);
        assertThat(deletedProject).isEmpty();
    }

    @Test
    @DisplayName("사용자와 이름으로 프로젝트를 검색할 수 있다")
    void findByUserAndNameContaining_ShouldReturnMatchingProjects() {
        // when
        List<Project> results = projectRepository.findByUserAndNameContaining(
                standardUser, "Project 1");

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Active Project 1");
    }

    @Test
    @DisplayName("사용자 ID와 상태로 프로젝트를 조회할 수 있다")
    void findByUserIdAndStatus_ShouldReturnMatchingProjects() {
        // when
        List<Project> activeProjects = projectRepository.findByUserIdAndStatus(standardUser.getId(), ProjectStatus.ACTIVE);
        List<Project> archivedProjects = projectRepository.findByUserIdAndStatus(standardUser.getId(), ProjectStatus.ARCHIVED);

        // then
        assertThat(activeProjects).hasSize(2);
        assertThat(activeProjects).extracting(Project::getStatus).containsOnly(ProjectStatus.ACTIVE);
        
        assertThat(archivedProjects).hasSize(1);
        assertThat(archivedProjects).extracting(Project::getStatus).containsOnly(ProjectStatus.ARCHIVED);
    }
}