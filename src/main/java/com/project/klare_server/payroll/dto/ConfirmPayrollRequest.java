package com.project.klare_server.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmPayrollRequest(
        @Schema(example = "123456")
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "must be a 6-digit code") String code) {
}
