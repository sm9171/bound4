package com.bound4.image.domain;

public enum SortDirection {
    ASC("asc"),
    DESC("desc");

    private final String value;

    SortDirection(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SortDirection fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DESC; // 기본값은 내림차순 (최신순)
        }
        
        for (SortDirection direction : values()) {
            if (direction.value.equalsIgnoreCase(value.trim())) {
                return direction;
            }
        }
        
        return DESC; // 유효하지 않은 값일 경우 기본값
    }

    public boolean isAscending() {
        return this == ASC;
    }

    public boolean isDescending() {
        return this == DESC;
    }
}