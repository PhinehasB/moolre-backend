package com.project.klare_server.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePayrollAutomationRequest(
        @Schema(example = "true")
        @NotNull Boolean automaticPayroll,

        @Schema(description = "Day of the month payroll runs (1-28)", example = "28")
        @Min(1) @Max(28) int payDate,

        @Schema(example = "true")
        @NotNull Boolean emailEstimateBeforeRun,

        @Schema(example = "true")
        @NotNull Boolean notifyEmployeesBeforePayday) {
}
