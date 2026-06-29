package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record DepositSalaryRequest(
        @DecimalMin(value = "1.00", message = "Enter an amount of at least GHS 1") BigDecimal amount) {
}
