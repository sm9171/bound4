package com.bound4.project.domain;

public enum Role {
    A("관리자", 4),
    B("프리미엄 사용자", 3),
    C("일반 사용자", 2),
    D("기본 사용자", 1);

    private final String description;
    private final int level;

    Role(String description, int level) {
        this.description = description;
        this.level = level;
    }

    public boolean hasPermission(String resource, String action, SubscriptionPlan subscriptionPlan) {
        return switch (this) {
            case A -> true;
            case B -> subscriptionPlan == SubscriptionPlan.PRO && level >= 3;
            case C -> (subscriptionPlan == SubscriptionPlan.PRO && level >= 2) || 
                     (subscriptionPlan == SubscriptionPlan.BASIC && isBasicAllowedAction(resource, action));
            case D -> subscriptionPlan == SubscriptionPlan.BASIC && isBasicAllowedAction(resource, action);
        };
    }

    private boolean isBasicAllowedAction(String resource, String action) {
        return "PROJECT".equals(resource) && ("READ".equals(action) || "CREATE".equals(action));
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherThan(Role other) {
        return this.level > other.level;
    }
}