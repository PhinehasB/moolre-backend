package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.NotBlank;

public record PersonalLoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password,
        boolean rememberMe) {
}
