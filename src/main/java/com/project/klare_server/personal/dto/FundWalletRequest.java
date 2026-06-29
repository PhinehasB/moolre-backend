package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FundWalletRequest(
        @NotNull(message = "Enter an amount") @DecimalMin(value = "1.00", message = "Enter an amount of at least GHS 1") BigDecimal amount,
        @NotBlank(message = "Enter the mobile money number to charge") @Size(max = 30) String payer) {
}
