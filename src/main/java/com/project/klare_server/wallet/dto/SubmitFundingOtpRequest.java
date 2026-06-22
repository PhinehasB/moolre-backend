package com.project.klare_server.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitFundingOtpRequest(
        @Schema(example = "123456")
        @NotBlank @Size(max = 10) String otpcode) {
}
