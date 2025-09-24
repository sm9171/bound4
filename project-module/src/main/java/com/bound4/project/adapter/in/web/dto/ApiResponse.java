package com.bound4.project.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String error,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공", data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, null, null, error, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message, String error) {
        return new ApiResponse<>(false, message, null, error, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, null, LocalDateTime.now());
    }
}