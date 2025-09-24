package com.bound4.project.application.service;

import com.bound4.project.adapter.in.web.dto.ProjectCreateRequest;
import com.bound4.project.adapter.in.web.dto.ProjectResponse;
import com.bound4.project.adapter.in.web.dto.ProjectUpdateRequest;
import com.bound4.project.adapter.out.persistence.ProjectRepository;
import com.bound4.project.adapter.out.persistence.UserRepository;
import com.bound4.project.config.TestDataConfig;
import com.bound4.project.domain.Project;
import com.bound4.project.domain.ProjectStatus;
import com.bound4.project.domain.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService 단위 테스트")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private ProjectService projectService;

    private TestDataConfig.TestUserFactory userFactory;
    private TestDataConfig.TestProjectFactory projectFactory;
    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        userFactory = new TestDataConfig.TestUserFactory();
        projectFactory = new TestDataConfig.TestProjectFactory();
        testUser = userFactory.createStandardUser();
        testProject = projectFactory.createTestProject(testUser);
    }

    @Test
    @DisplayName("프로젝트를 성공적으로 생성할 수 있다")
    void createProject_WithValidRequest_ShouldSucceed() {
        // given
        Long userId = 1L;
        ProjectCreateRequest request = new ProjectCreateRequest("New Project", "Description");
        Project savedProject = projectFactory.createProject("New Project", "Description", testUser);

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(projectRepository.existsByUserAndName(testUser, "New Project")).willReturn(false);
        given(projectRepository.save(any(Project.class))).willReturn(savedProject);

        // when
        ProjectResponse response = projectService.createProject(userId, request);

        // then
        assertThat(response.name()).isEqualTo("New Project");
        assertThat(response.description()).isEqualTo("Description");
        assertThat(response.userId()).isEqualTo(testUser.getId());

        verify(permissionService).validateProjectCreationLimit(userId);
        verify(permissionService).enforcePermission(userId, "PROJECT", "CREATE");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("중복된 프로젝트 이름으로 생성하면 예외가 발생한다")
    void createProject_WithDuplicateName_ShouldThrowException() {
        // given
        Long userId = 1L;
        ProjectCreateRequest request = new ProjectCreateRequest("Existing Project", "Description");

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(projectRepository.existsByUserAndName(testUser, "Existing Project")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> projectService.createProject(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 이름의 프로젝트가 이미 존재합니다");

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 프로젝트를 생성하면 예외가 발생한다")
    void createProject_WithNonExistentUser_ShouldThrowException() {
        // given
        Long userId = 999L;
        ProjectCreateRequest request = new ProjectCreateRequest("New Project", "Description");

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> projectService.createProject(userId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("프로젝트를 성공적으로 조회할 수 있다")
    void getProject_WithValidRequest_ShouldSucceed() {
        // given
        Long userId = 1L;
        Long projectId = 1L;

        given(projectRepository.findByIdWithUser(projectId)).willReturn(Optional.of(testProject));

        // when
        ProjectResponse response = projectService.getProject(userId, projectId);

        // then
        assertThat(response.name()).isEqualTo(testProject.getName());
        assertThat(response.description()).isEqualTo(testProject.getDescription());

        verify(permissionService).enforceProjectAccess(userId, projectId, "READ");
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트를 조회하면 예외가 발생한다")
    void getProject_WithNonExistentProject_ShouldThrowException() {
        // given
        Long userId = 1L;
        Long projectId = 999L;

        given(projectRepository.findByIdWithUser(projectId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> projectService.getProject(userId, projectId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("프로젝트를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("프로젝트를 성공적으로 수정할 수 있다")
    void updateProject_WithValidRequest_ShouldSucceed() {
        // given
        Long userId = 1L;
        Long projectId = 1L;
        ProjectUpdateRequest request = new ProjectUpdateRequest("Updated Name", "Updated Description");

        given(projectRepository.findByIdWithUser(projectId)).willReturn(Optional.of(testProject));
        given(projectRepository.existsByUserAndName(testProject.getUser(), "Updated Name")).willReturn(false);
        given(projectRepository.save(testProject)).willReturn(testProject);

        // when
        ProjectResponse response = projectService.updateProject(userId, projectId, request);

        // then
        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.description()).isEqualTo("Updated Description");

        verify(permissionService).enforceProjectAccess(userId, projectId, "UPDATE");
        verify(projectRepository).save(testProject);
    }

    @Test
    @DisplayName("변경사항이 없으면 저장하지 않는다")
    void updateProject_WithNoChanges_ShouldNotSave() {
        // given
        Long userId = 1L;
        Long projectId = 1L;
        ProjectUpdateRequest request = new ProjectUpdateRequest(null, null);

        given(projectRepository.findByIdWithUser(projectId)).willReturn(Optional.of(testProject));

        // when
        ProjectResponse response = projectService.updateProject(userId, projectId, request);

        // then
        assertThat(response.name()).isEqualTo(testProject.getName());
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("프로젝트를 성공적으로 삭제할 수 있다")
    void deleteProject_WithValidRequest_ShouldSucceed() {
        // given
        Long userId = 1L;
        Long projectId = 1L;

        given(projectRepository.findByIdWithUser(projectId)).willReturn(Optional.of(testProject));
        given(projectRepository.save(testProject)).willReturn(testProject);

        // when
        projectService.deleteProject(userId, projectId);

        // then
        assertThat(testProject.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        verify(permissionService).enforceProjectAccess(userId, projectId, "DELETE");
        verify(projectRepository).save(testProject);
    }

    @Test
    @DisplayName("프로젝트 목록을 성공적으로 조회할 수 있다")
    void getProjects_WithValidRequest_ShouldSucceed() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        User standardUser = userFactory.createStandardUser();
        
        Project project1 = projectFactory.createProject("Project 1", "Desc 1", standardUser);
        Project project2 = projectFactory.createProject("Project 2", "Desc 2", standardUser);
        Page<Project> projectPage = new PageImpl<>(Arrays.asList(project1, project2), pageable, 2);

        given(userRepository.findById(userId)).willReturn(Optional.of(standardUser));
        given(projectRepository.findByUserAndStatus(standardUser, ProjectStatus.ACTIVE, pageable))
                .willReturn(projectPage);

        // when
        Page<ProjectResponse> response = projectService.getProjects(userId, pageable);

        // then
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).name()).isEqualTo("Project 1");
        assertThat(response.getContent().get(1).name()).isEqualTo("Project 2");

        verify(permissionService).enforcePermission(userId, "PROJECT", "READ");
    }

    @Test
    @DisplayName("관리자는 모든 프로젝트를 조회할 수 있다")
    void getProjects_WithAdmin_ShouldReturnAllProjects() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        User admin = userFactory.createAdminUser();
        
        Project project1 = projectFactory.createProject("Project 1", "Desc 1", testUser);
        Project project2 = projectFactory.createProject("Project 2", "Desc 2", testUser);
        Page<Project> projectPage = new PageImpl<>(Arrays.asList(project1, project2), pageable, 2);

        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
        given(projectRepository.findByStatus(ProjectStatus.ACTIVE, pageable)).willReturn(projectPage);

        // when
        Page<ProjectResponse> response = projectService.getProjects(adminId, pageable);

        // then
        assertThat(response.getContent()).hasSize(2);
        verify(projectRepository).findByStatus(ProjectStatus.ACTIVE, pageable);
    }

    @Test
    @DisplayName("활성 프로젝트 개수를 조회할 수 있다")
    void getActiveProjectCount_ShouldReturnCorrectCount() {
        // given
        Long userId = 1L;
        long expectedCount = 5L;

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(projectRepository.countByUserAndStatus(testUser, ProjectStatus.ACTIVE)).willReturn(expectedCount);

        // when
        long actualCount = projectService.getActiveProjectCount(userId);

        // then
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("권한 검증이 실패하면 예외가 발생한다")
    void createProject_WithPermissionDenied_ShouldThrowException() {
        // given
        Long userId = 1L;
        ProjectCreateRequest request = new ProjectCreateRequest("New Project", "Description");

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        willThrow(new AccessDeniedException("권한이 없습니다"))
                .given(permissionService).enforcePermission(userId, "PROJECT", "CREATE");

        // when & then
        assertThatThrownBy(() -> projectService.createProject(userId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("권한이 없습니다");

        verify(projectRepository, never()).save(any(Project.class));
    }
}