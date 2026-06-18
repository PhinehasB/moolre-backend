package com.project.klare_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "ama@techcorp.com")
        @NotBlank @Email @Size(max = 320) String email,

        @Schema(example = "Str0ng!Pass")
        @NotBlank @Size(max = 72) String password,

        @Schema(description = "Keep me signed in (issues a long-lived refresh token)", example = "false")
        boolean rememberMe) {
}
