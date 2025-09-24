package com.bound4.project.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_user_email")
    private String targetUserEmail;

    @Column(name = "old_value", length = 1000)
    private String oldValue;

    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Column(length = 500)
    private String reason;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(AuditAction action, String entityType, Long entityId,
                   Long adminUserId, String adminEmail, String reason) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.adminUserId = adminUserId;
        this.adminEmail = adminEmail;
        this.reason = reason;
    }

    public static AuditLog rolePolicyChanged(Long adminUserId, String adminEmail,
                                           Long policyId, String oldValue, String newValue, String reason) {
        AuditLog log = new AuditLog(AuditAction.ROLE_POLICY_UPDATED, "RolePolicy", policyId, 
                                  adminUserId, adminEmail, reason);
        log.oldValue = oldValue;
        log.newValue = newValue;
        return log;
    }

    public static AuditLog userRoleChanged(Long adminUserId, String adminEmail,
                                         Long targetUserId, String targetUserEmail,
                                         String oldRole, String newRole, String reason) {
        AuditLog log = new AuditLog(AuditAction.USER_ROLE_UPDATED, "User", targetUserId,
                                  adminUserId, adminEmail, reason);
        log.targetUserId = targetUserId;
        log.targetUserEmail = targetUserEmail;
        log.oldValue = oldRole;
        log.newValue = newRole;
        return log;
    }

    public static AuditLog policyBackupCreated(Long adminUserId, String adminEmail, String backupId) {
        AuditLog log = new AuditLog(AuditAction.POLICY_BACKUP_CREATED, "PolicyBackup", null,
                                  adminUserId, adminEmail, "정책 백업 생성");
        log.newValue = backupId;
        return log;
    }

    public void setClientInfo(String clientIp, String userAgent) {
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public String getTargetUserEmail() {
        return targetUserEmail;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getReason() {
        return reason;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", action=" + action +
                ", entityType='" + entityType + '\'' +
                ", adminEmail='" + adminEmail + '\'' +
                ", targetUserEmail='" + targetUserEmail + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}