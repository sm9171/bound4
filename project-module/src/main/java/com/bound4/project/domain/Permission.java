package com.bound4.project.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "permissions", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"resource", "action", "role", "subscription_plan"},
           name = "uk_permission_resource_action_role_plan"
       ))
@EntityListeners(AuditingEntityListener.class)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "리소스는 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resource resource;

    @NotNull(message = "액션은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "역할은 필수입니다")
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "구독 플랜은 필수입니다")
    @Column(name = "subscription_plan", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(nullable = false)
    private boolean allowed = true;

    @Column(length = 200)
    private String description;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Permission() {
    }

    public Permission(Resource resource, Action action, Role role, SubscriptionPlan subscriptionPlan) {
        this.resource = Objects.requireNonNull(resource, "리소스는 필수입니다");
        this.action = Objects.requireNonNull(action, "액션은 필수입니다");
        this.role = Objects.requireNonNull(role, "역할은 필수입니다");
        this.subscriptionPlan = Objects.requireNonNull(subscriptionPlan, "구독 플랜은 필수입니다");
        this.allowed = true;
    }

    public Permission(Resource resource, Action action, Role role, SubscriptionPlan subscriptionPlan, boolean allowed) {
        this(resource, action, role, subscriptionPlan);
        this.allowed = allowed;
    }

    public void allow() {
        this.allowed = true;
    }

    public void deny() {
        this.allowed = false;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public boolean matches(Resource resource, Action action, Role role, SubscriptionPlan subscriptionPlan) {
        return Objects.equals(this.resource, resource) &&
               Objects.equals(this.action, action) &&
               Objects.equals(this.role, role) &&
               Objects.equals(this.subscriptionPlan, subscriptionPlan);
    }

    public boolean isApplicableFor(User user) {
        return Objects.equals(this.role, user.getRole()) &&
               Objects.equals(this.subscriptionPlan, user.getSubscriptionPlan());
    }

    public Long getId() {
        return id;
    }

    public Resource getResource() {
        return resource;
    }

    public Action getAction() {
        return action;
    }

    public Role getRole() {
        return role;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getDescription() {
        return description;
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
        if (!(o instanceof Permission that)) return false;
        return Objects.equals(resource, that.resource) &&
               Objects.equals(action, that.action) &&
               Objects.equals(role, that.role) &&
               Objects.equals(subscriptionPlan, that.subscriptionPlan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, action, role, subscriptionPlan);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", resource=" + resource +
                ", action=" + action +
                ", role=" + role +
                ", subscriptionPlan=" + subscriptionPlan +
                ", allowed=" + allowed +
                '}';
    }
}