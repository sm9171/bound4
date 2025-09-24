package com.bound4.project.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "projects")
@EntityListeners(AuditingEntityListener.class)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "프로젝트 이름은 필수입니다")
    @Size(min = 1, max = 100, message = "프로젝트 이름은 1~100자 사이여야 합니다")
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500, message = "프로젝트 설명은 500자를 초과할 수 없습니다")
    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_user"))
    @NotNull(message = "프로젝트 소유자는 필수입니다")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    protected Project() {
    }

    public Project(String name, String description, User user) {
        validateName(name);
        validateUser(user);
        this.name = name.trim();
        this.description = description != null ? description.trim() : null;
        this.user = user;
        this.status = ProjectStatus.ACTIVE;
    }

    public void updateName(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

    public void updateDescription(String newDescription) {
        if (newDescription != null && newDescription.length() > 500) {
            throw new IllegalArgumentException("프로젝트 설명은 500자를 초과할 수 없습니다");
        }
        this.description = newDescription != null ? newDescription.trim() : null;
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
    }

    public void activate() {
        this.status = ProjectStatus.ACTIVE;
    }

    public boolean isOwnedBy(User user) {
        return Objects.equals(this.user, user);
    }

    public boolean isActive() {
        return status == ProjectStatus.ACTIVE;
    }

    public boolean canBeAccessedBy(User requestUser) {
        if (isOwnedBy(requestUser)) {
            return true;
        }
        
        Role userRole = requestUser.getRole();
        return userRole == Role.A || 
               (userRole == Role.B && requestUser.getSubscriptionPlan() == SubscriptionPlan.PRO);
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("프로젝트 이름은 필수입니다");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("프로젝트 이름은 100자를 초과할 수 없습니다");
        }
    }

    private void validateUser(User user) {
        Objects.requireNonNull(user, "프로젝트 소유자는 필수입니다");
        if (!user.getSubscriptionPlan().canCreateProject(user.getProjects().size())) {
            throw new IllegalStateException("구독 플랜의 프로젝트 생성 한도를 초과했습니다");
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public User getUser() {
        return user;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project project)) return false;
        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", userId=" + getUserId() +
                ", createdAt=" + createdAt +
                '}';
    }
}