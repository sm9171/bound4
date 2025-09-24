package com.bound4.project.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notifications")
@EntityListeners(AuditingEntityListener.class)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "reference_id")
    private String referenceId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected UserNotification() {
    }

    public UserNotification(Long userId, NotificationType type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public static UserNotification roleChanged(Long userId, Long adminUserId, String oldRole, String newRole, String reason) {
        UserNotification notification = new UserNotification(
                userId,
                NotificationType.ROLE_CHANGED,
                "역할이 변경되었습니다",
                String.format("관리자에 의해 역할이 %s에서 %s로 변경되었습니다. 사유: %s", oldRole, newRole, reason)
        );
        notification.adminUserId = adminUserId;
        return notification;
    }

    public static UserNotification permissionChanged(Long userId, Long adminUserId, String resource, String action, boolean allowed) {
        String status = allowed ? "허용" : "거부";
        UserNotification notification = new UserNotification(
                userId,
                NotificationType.PERMISSION_CHANGED,
                "권한이 변경되었습니다",
                String.format("관리자에 의해 %s %s 권한이 %s되었습니다", resource, action, status)
        );
        notification.adminUserId = adminUserId;
        return notification;
    }

    public static UserNotification systemNotice(Long userId, String title, String message) {
        return new UserNotification(userId, NotificationType.SYSTEM_NOTICE, title, message);
    }

    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    @Override
    public String toString() {
        return "UserNotification{" +
                "id=" + id +
                ", userId=" + userId +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", read=" + read +
                ", createdAt=" + createdAt +
                '}';
    }
}