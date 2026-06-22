package com.project.klare_server.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FundWalletRequest(
        @Schema(description = "Mobile money number to charge", example = "0505678589")
        @NotBlank @Size(max = 20)
        @Pattern(regexp = "^\\+?[0-9\\s-]{7,20}$", message = "must be a valid mobile money number") String payer,

        @Schema(example = "1.00")
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "must be a valid amount") BigDecimal amount) {
}
