package com.project.klare_server.employee.dto;

import com.project.klare_server.employee.domain.EmployeeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateEmployeeRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^\\+?[0-9\\s-]{7,20}$", message = "must be a valid phone number") String phone,
        @NotBlank @Size(max = 120) String role,
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "must be a valid amount") BigDecimal monthlySalary,
        @NotNull EmployeeStatus status) {
}
