package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank(message = "A push token is required") @Size(max = 255) String expoPushToken,
        @Size(max = 20) String platform) {
}
