package com.project.klare_server.employee.dto;

import java.util.List;

public record ImportResultResponse(
        int totalRows,
        int imported,
        int skipped,
        List<RowError> errors) {

    public record RowError(int row, String email, String message) {
    }
}
