package com.project.klare_server.employee.dto;

public record EmployeeStatsResponse(long total, long active, long pending, long suspended) {
}
