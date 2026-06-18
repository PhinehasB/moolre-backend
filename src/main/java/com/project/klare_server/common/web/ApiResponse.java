package com.project.klare_server.common.web;

import com.project.klare_server.common.error.ApiError;
import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }
}
