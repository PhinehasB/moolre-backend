package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonalSignupRequest(
        @NotBlank(message = "First name is required") @Size(max = 100) String firstName,
        @NotBlank(message = "Last name is required") @Size(max = 100) String lastName,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
        @Size(max = 30) String phone,
        @NotBlank(message = "Password is required") String password,
        @NotBlank(message = "Please confirm your password") String confirmPassword,
        boolean acceptedTerms) {
}
