package com.bound4.project.adapter.in.web.exception;

public class ProjectException extends RuntimeException {

    private final ProjectExceptionType type;

    public ProjectException(ProjectExceptionType type, String message) {
        super(message);
        this.type = type;
    }

    public ProjectException(ProjectExceptionType type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public ProjectExceptionType getType() {
        return type;
    }

    public static ProjectException invalidRequest(String message) {
        return new ProjectException(ProjectExceptionType.INVALID_REQUEST, message);
    }

    public static ProjectException notFound(String message) {
        return new ProjectException(ProjectExceptionType.NOT_FOUND, message);
    }

    public static ProjectException accessDenied(String message) {
        return new ProjectException(ProjectExceptionType.ACCESS_DENIED, message);
    }

    public static ProjectException conflict(String message) {
        return new ProjectException(ProjectExceptionType.CONFLICT, message);
    }

    public static ProjectException subscriptionLimitExceeded(String message) {
        return new ProjectException(ProjectExceptionType.SUBSCRIPTION_LIMIT_EXCEEDED, message);
    }

    public enum ProjectExceptionType {
        INVALID_REQUEST,
        NOT_FOUND,
        ACCESS_DENIED,
        CONFLICT,
        SUBSCRIPTION_LIMIT_EXCEEDED
    }
}