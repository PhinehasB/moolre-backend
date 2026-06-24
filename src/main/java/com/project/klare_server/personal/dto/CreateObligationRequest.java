package com.project.klare_server.personal.dto;

import com.project.klare_server.personal.domain.ObligationCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateObligationRequest(
        @NotBlank(message = "Tell us what it's for") @Size(max = 120) String name,
        @NotNull(message = "Enter an amount") @DecimalMin(value = "0.01", message = "Enter an amount") BigDecimal amount,
        @NotNull(message = "Pick a category") ObligationCategory category,
        @NotBlank(message = "Choose where it should go") @Size(max = 40) String network,
        @Size(max = 60) String recipientNumber) {
}
