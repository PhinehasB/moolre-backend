package com.project.klare_server.auth.dto;

import com.project.klare_server.common.validation.StrongPassword;
import com.project.klare_server.company.domain.Industry;
import com.project.klare_server.company.domain.PayrollBand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCompanyRequest(
        @NotNull @Valid Company company,
        @NotNull @Valid Administrator administrator,
        @NotNull @Valid Consents consents) {

    public record Company(
            @Schema(example = "TechCorp Ltd")
            @NotBlank @Size(max = 200) String name,

            @Schema(example = "CS-000-000")
            @NotBlank @Size(max = 100) String registrationNumber,

            @Schema(example = "TECHNOLOGY")
            @NotNull Industry industry,

            @Schema(example = "UNDER_10K")
            @NotNull PayrollBand expectedMonthlyPayroll) {
    }

    public record Administrator(
            @Schema(example = "Ama")
            @NotBlank @Size(max = 100) String firstName,

            @Schema(example = "Owusu")
            @NotBlank @Size(max = 100) String lastName,

            @Schema(example = "ama@techcorp.com")
            @NotBlank @Email @Size(max = 320) String email,

            @Schema(example = "+233054857203")
            @NotBlank @Size(max = 30)
            @Pattern(regexp = "^\\+?[0-9\\s-]{7,20}$", message = "must be a valid phone number") String phone,

            @Schema(example = "Str0ng!Pass")
            @NotBlank @StrongPassword String password) {
    }

    public record Consents(
            @AssertTrue(message = "You must accept the Terms and Privacy Policy")
            @NotNull Boolean acceptedTerms,

            @AssertTrue(message = "You must authorize Klare to move funds through Moolre")
            @NotNull Boolean authorizedFundMovement) {
    }
}
