package com.project.klare_server.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateEmployeeRequest(
        @Schema(example = "Kwame")
        @NotBlank @Size(max = 100) String firstName,

        @Schema(example = "Essien")
        @NotBlank @Size(max = 100) String lastName,

        @Schema(example = "kwame@techcorp.com")
        @NotBlank @Email @Size(max = 320) String email,

        @Schema(example = "+233241112233")
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^\\+?[0-9\\s-]{7,20}$", message = "must be a valid phone number") String phone,

        @Schema(example = "Software Engineer")
        @Size(max = 120) String jobTitle,

        @Schema(example = "5000.00")
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "must be a valid amount") BigDecimal monthlySalary,

        @Schema(description = "Send login credentials and app invitation now", example = "true")
        Boolean sendInvitation) {
}
