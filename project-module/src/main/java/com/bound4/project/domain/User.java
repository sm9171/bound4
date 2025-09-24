package com.bound4.project.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email(message = "유효한 이메일 형식이어야 합니다")
    @NotBlank(message = "이메일은 필수입니다")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "역할은 필수입니다")
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "구독 플랜은 필수입니다")
    @Column(nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
    }

    public User(String email, String password, Role role, SubscriptionPlan subscriptionPlan) {
        validateEmail(email);
        validatePassword(password);
        this.email = email;
        this.password = password;
        this.role = Objects.requireNonNull(role, "역할은 null일 수 없습니다");
        this.subscriptionPlan = Objects.requireNonNull(subscriptionPlan, "구독 플랜은 null일 수 없습니다");
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "역할은 null일 수 없습니다");
    }

    public void upgradeSubscriptionPlan(SubscriptionPlan newPlan) {
        this.subscriptionPlan = Objects.requireNonNull(newPlan, "구독 플랜은 null일 수 없습니다");
    }

    public void changePassword(String newPassword) {
        validatePassword(newPassword);
        this.password = newPassword;
    }

    public boolean hasPermission(String resource, String action) {
        return role.hasPermission(resource, action, subscriptionPlan);
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("유효한 이메일 형식이어야 합니다");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다");
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public List<Project> getProjects() {
        return List.copyOf(projects);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", subscriptionPlan=" + subscriptionPlan +
                ", createdAt=" + createdAt +
                '}';
    }
}