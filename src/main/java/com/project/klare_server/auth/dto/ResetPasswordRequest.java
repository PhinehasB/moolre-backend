package com.project.klare_server.auth.dto;

import com.project.klare_server.common.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @Schema(description = "Reset token from the password reset email")
        @NotBlank String token,

        @Schema(example = "N3w!Str0ng")
        @NotBlank @StrongPassword String newPassword) {
}
