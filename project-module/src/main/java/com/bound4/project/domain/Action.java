package com.bound4.project.domain;

public enum Action {
    CREATE("생성"),
    READ("조회"),
    UPDATE("수정"),
    DELETE("삭제"),
    MANAGE("관리"),
    EXECUTE("실행");

    private final String description;

    Action(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isWriteOperation() {
        return this == CREATE || this == UPDATE || this == DELETE || this == MANAGE;
    }

    public boolean isReadOperation() {
        return this == READ;
    }
}