package com.project.klare_server.settings.dto;

import com.project.klare_server.common.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(example = "Str0ng!Pass")
        @NotBlank @Size(max = 72) String currentPassword,

        @Schema(example = "N3w!Str0ngPass")
        @NotBlank @StrongPassword String newPassword) {
}
