package com.roykhan.dddorderboundary.common.response;

public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, "success", message, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "success", message, data);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}
