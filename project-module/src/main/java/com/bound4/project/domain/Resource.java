package com.bound4.project.domain;

public enum Resource {
    PROJECT("프로젝트"),
    USER("사용자"),
    PERMISSION("권한"),
    ROLE_POLICY("역할 정책"),
    SYSTEM("시스템");

    private final String description;

    Resource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}