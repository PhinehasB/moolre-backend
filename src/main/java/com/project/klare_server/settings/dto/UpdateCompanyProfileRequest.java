package com.project.klare_server.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyProfileRequest(
        @Schema(example = "TechCorp Ltd")
        @NotBlank @Size(max = 200) String companyName,

        @Schema(example = "CS-244-0091")
        @NotBlank @Size(max = 100) String registrationNumber,

        @Schema(example = "ama@techcorp.com")
        @NotBlank @Email @Size(max = 320) String adminEmail) {
}
