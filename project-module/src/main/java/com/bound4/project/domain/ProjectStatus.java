package com.bound4.project.domain;

public enum ProjectStatus {
    ACTIVE("활성"),
    ARCHIVED("보관됨"),
    DELETED("삭제됨");

    private final String description;

    ProjectStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAccessible() {
        return this == ACTIVE;
    }
}