package com.project.klare_server.common.error;

import java.util.List;

public record ApiError(
        String code,
        String message,
        List<FieldViolation> violations,
        String traceId) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(String code, String message, String traceId) {
        return new ApiError(code, message, null, traceId);
    }

    public static ApiError of(String code, String message, List<FieldViolation> violations, String traceId) {
        return new ApiError(code, message, violations, traceId);
    }
}
