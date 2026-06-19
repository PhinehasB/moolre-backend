package com.project.klare_server.auth.web;

import com.project.klare_server.auth.dto.AuthenticationResponse;
import com.project.klare_server.auth.dto.ForgotPasswordRequest;
import com.project.klare_server.auth.dto.LoginRequest;
import com.project.klare_server.auth.dto.LogoutRequest;
import com.project.klare_server.auth.dto.MessageResponse;
import com.project.klare_server.auth.dto.RefreshTokenRequest;
import com.project.klare_server.auth.dto.RegisterCompanyRequest;
import com.project.klare_server.auth.dto.RegistrationOptionsResponse;
import com.project.klare_server.auth.dto.ResetPasswordRequest;
import com.project.klare_server.auth.service.AuthService;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.common.web.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/company")
@Tag(name = "Company Authentication", description = "Registration and sign-in for Klare Business companies")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a company and its admin",
            description = "Creates the company, its owner administrator account and consents, then returns access and refresh tokens. "
                    + "Send an Idempotency-Key header to make retries safe.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(
            @Valid @RequestBody RegisterCompanyRequest request,
            @Parameter(description = "Optional unique key (max 255 chars) to safely retry this request without creating duplicates")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = ClientIpResolver.resolve(httpRequest);
        AuthenticationResponse response = authService.registerCompany(request, userAgent, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Sign in to a company account",
            description = "Authenticates an administrator and returns access and refresh tokens. "
                    + "Set rememberMe to true for a long-lived session. Repeated failures temporarily lock the account.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = ClientIpResolver.resolve(httpRequest);
        AuthenticationResponse response = authService.login(request, userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Request a password reset",
            description = "Sends a password reset link if the email belongs to an active account. "
                    + "Always returns the same response so that account existence is never revealed.")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                new MessageResponse("If an account exists for that email, a password reset link has been sent.")));
    }

    @Operation(
            summary = "Reset a password",
            description = "Sets a new password using a valid reset token. The token is single-use and all existing sessions are signed out.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                new MessageResponse("Your password has been reset. Please sign in with your new password.")));
    }

    @Operation(
            summary = "Refresh the access token",
            description = "Exchanges a valid refresh token for a new access token and a rotated refresh token. "
                    + "The previous refresh token is revoked; reusing it signs out all sessions.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = ClientIpResolver.resolve(httpRequest);
        AuthenticationResponse response = authService.refresh(request.refreshToken(), userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Sign out",
            description = "Revokes the supplied refresh token. Always succeeds so it is safe to call on logout.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(new MessageResponse("Signed out.")));
    }

    @Operation(
            summary = "Get registration form options",
            description = "Returns the available industry and expected monthly payroll options for the registration form.")
    @GetMapping("/registration-options")
    public ResponseEntity<ApiResponse<RegistrationOptionsResponse>> registrationOptions() {
        return ResponseEntity.ok(ApiResponse.ok(RegistrationOptionsResponse.build()));
    }
}
