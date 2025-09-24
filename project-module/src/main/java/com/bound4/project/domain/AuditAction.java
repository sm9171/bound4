package com.bound4.project.domain;

public enum AuditAction {
    USER_ROLE_UPDATED("사용자 역할 변경"),
    ROLE_POLICY_UPDATED("역할 정책 변경"),
    ROLE_POLICY_CREATED("역할 정책 생성"),
    ROLE_POLICY_DELETED("역할 정책 삭제"),
    POLICY_BACKUP_CREATED("정책 백업 생성"),
    POLICY_BACKUP_RESTORED("정책 백업 복원"),
    PERMISSION_GRANTED("권한 부여"),
    PERMISSION_REVOKED("권한 취소"),
    USER_CREATED("사용자 생성"),
    USER_DELETED("사용자 삭제"),
    SYSTEM_SETTING_CHANGED("시스템 설정 변경");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}