package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Select a network") @Size(max = 40) String network,
        @NotBlank(message = "Enter the recipient's phone number") @Size(max = 60) String phone,
        @NotNull(message = "Enter an amount") @DecimalMin(value = "1.00", message = "Enter an amount of at least GHS 1") BigDecimal amount) {
}
