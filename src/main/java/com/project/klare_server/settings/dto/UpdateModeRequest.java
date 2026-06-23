package com.project.klare_server.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateModeRequest(
        @Schema(description = "true for Live (real Moolre), false for Sandbox (simulated)", example = "true")
        @NotNull Boolean live) {
}
