package com.bound4.project.domain;

public enum SubscriptionPlan {
    BASIC("기본 플랜", 1, 100),
    PRO("프로 플랜", 5, 10000);

    private final String description;
    private final int maxProjects;
    private final int maxStorageGB;

    SubscriptionPlan(String description, int maxProjects, int maxStorageGB) {
        this.description = description;
        this.maxProjects = maxProjects;
        this.maxStorageGB = maxStorageGB;
    }

    public boolean canCreateProject(int currentProjectCount) {
        return currentProjectCount < maxProjects;
    }

    public boolean hasStorageLimit(int currentStorageGB) {
        return currentStorageGB < maxStorageGB;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxProjects() {
        return maxProjects;
    }

    public int getMaxStorageGB() {
        return maxStorageGB;
    }

    public boolean isUpgradeFrom(SubscriptionPlan other) {
        return this.ordinal() > other.ordinal();
    }
}