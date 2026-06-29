package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitFundingOtpRequest(
        @NotBlank(message = "The funding reference is required") @Size(max = 80) String externalRef,
        @NotBlank(message = "Enter the OTP sent to your phone") @Size(max = 12) String otpcode) {
}
