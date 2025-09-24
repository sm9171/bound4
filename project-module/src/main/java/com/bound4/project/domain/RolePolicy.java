package com.bound4.project.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "role_policies",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"role", "resource", "action", "subscription_plan"},
           name = "uk_role_policy_role_resource_action_plan"
       ))
@EntityListeners(AuditingEntityListener.class)
public class RolePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "역할은 필수입니다")
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "리소스는 필수입니다")
    @Column(nullable = false)
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "액션은 필수입니다")
    @Column(nullable = false)
    private Action action;

    @Column(nullable = false)
    private boolean allowed;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "구독 플랜은 필수입니다")
    @Column(name = "subscription_plan", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(length = 200)
    private String reason;

    @Column(name = "is_system_policy", nullable = false)
    private boolean systemPolicy = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    protected RolePolicy() {
    }

    public RolePolicy(Role role, Resource resource, Action action, boolean allowed, SubscriptionPlan subscriptionPlan) {
        this.role = Objects.requireNonNull(role, "역할은 필수입니다");
        this.resource = Objects.requireNonNull(resource, "리소스는 필수입니다");
        this.action = Objects.requireNonNull(action, "액션은 필수입니다");
        this.allowed = allowed;
        this.subscriptionPlan = Objects.requireNonNull(subscriptionPlan, "구독 플랜은 필수입니다");
        this.systemPolicy = false;
    }

    public static RolePolicy createSystemPolicy(Role role, Resource resource, Action action, boolean allowed, SubscriptionPlan subscriptionPlan) {
        RolePolicy policy = new RolePolicy(role, resource, action, allowed, subscriptionPlan);
        policy.systemPolicy = true;
        return policy;
    }

    public void updatePermission(boolean allowed, String reason) {
        if (systemPolicy) {
            throw new IllegalStateException("시스템 정책은 수정할 수 없습니다");
        }
        this.allowed = allowed;
        this.reason = reason;
    }

    public void updateReason(String reason) {
        if (systemPolicy) {
            throw new IllegalStateException("시스템 정책은 수정할 수 없습니다");
        }
        this.reason = reason;
    }

    public boolean isApplicableFor(User user, Resource resource, Action action) {
        return Objects.equals(this.role, user.getRole()) &&
               Objects.equals(this.resource, resource) &&
               Objects.equals(this.action, action) &&
               Objects.equals(this.subscriptionPlan, user.getSubscriptionPlan());
    }

    public boolean matches(Role role, Resource resource, Action action, SubscriptionPlan subscriptionPlan) {
        return Objects.equals(this.role, role) &&
               Objects.equals(this.resource, resource) &&
               Objects.equals(this.action, action) &&
               Objects.equals(this.subscriptionPlan, subscriptionPlan);
    }

    public boolean canBeModified() {
        return !systemPolicy;
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public Resource getResource() {
        return resource;
    }

    public Action getAction() {
        return action;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public String getReason() {
        return reason;
    }

    public boolean isSystemPolicy() {
        return systemPolicy;
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
        if (!(o instanceof RolePolicy that)) return false;
        return Objects.equals(role, that.role) &&
               Objects.equals(resource, that.resource) &&
               Objects.equals(action, that.action) &&
               Objects.equals(subscriptionPlan, that.subscriptionPlan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, resource, action, subscriptionPlan);
    }

    @Override
    public String toString() {
        return "RolePolicy{" +
                "id=" + id +
                ", role=" + role +
                ", resource=" + resource +
                ", action=" + action +
                ", allowed=" + allowed +
                ", subscriptionPlan=" + subscriptionPlan +
                ", systemPolicy=" + systemPolicy +
                '}';
    }
}