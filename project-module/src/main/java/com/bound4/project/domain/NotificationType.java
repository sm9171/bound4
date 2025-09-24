package com.bound4.project.domain;

public enum NotificationType {
    ROLE_CHANGED("역할 변경"),
    PERMISSION_CHANGED("권한 변경"),
    SYSTEM_NOTICE("시스템 공지"),
    POLICY_UPDATED("정책 업데이트"),
    ACCOUNT_SUSPENDED("계정 정지"),
    ACCOUNT_REACTIVATED("계정 재활성화"),
    SUBSCRIPTION_CHANGED("구독 플랜 변경"),
    SECURITY_ALERT("보안 알림");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}