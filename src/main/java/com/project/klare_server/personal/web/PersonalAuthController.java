package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.common.web.ClientIpResolver;
import com.project.klare_server.personal.dto.ActivateAccountRequest;
import com.project.klare_server.personal.dto.PersonalAccountResponse;
import com.project.klare_server.personal.dto.PersonalAuthResponse;
import com.project.klare_server.personal.dto.PersonalLoginRequest;
import com.project.klare_server.personal.dto.PersonalLogoutRequest;
import com.project.klare_server.personal.dto.PersonalRefreshRequest;
import com.project.klare_server.personal.dto.PersonalSignupRequest;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal/auth")
@Tag(name = "Personal Authentication", description = "Sign-up and sign-in for Klare individuals and employees on the mobile app")
public class PersonalAuthController {

    private final PersonalAuthService personalAuthService;

    public PersonalAuthController(PersonalAuthService personalAuthService) {
        this.personalAuthService = personalAuthService;
    }

    @Operation(
            summary = "Create a personal account",
            description = "For individuals who want to use Klare without an employer. Creates the account from full name, "
                    + "email, and password, and returns tokens immediately.")
    @SecurityRequirements
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<PersonalAuthResponse>> signup(
            @Valid @RequestBody PersonalSignupRequest request, HttpServletRequest httpRequest) {
        PersonalAuthResponse response = personalAuthService.signup(
                request, httpRequest.getHeader(HttpHeaders.USER_AGENT), ClientIpResolver.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Sign in",
            description = "Employees sign in with the username their employer emailed them; individuals sign in with the "
                    + "email they signed up with. If mustChangePassword is true, send the user to activate their account.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<PersonalAuthResponse>> login(
            @Valid @RequestBody PersonalLoginRequest request, HttpServletRequest httpRequest) {
        PersonalAuthResponse response = personalAuthService.login(
                request, httpRequest.getHeader(HttpHeaders.USER_AGENT), ClientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Activate account by setting a new password",
            description = "First sign-in step for employer-provisioned employees. Replaces the temporary password and "
                    + "returns fresh tokens with mustChangePassword set to false.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<PersonalAuthResponse>> activate(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @Valid @RequestBody ActivateAccountRequest request,
            HttpServletRequest httpRequest) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        PersonalAuthResponse response = personalAuthService.activate(
                principal, request, httpRequest.getHeader(HttpHeaders.USER_AGENT), ClientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Rotate tokens", description = "Exchanges a valid refresh token for a new access and refresh token pair.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<PersonalAuthResponse>> refresh(
            @Valid @RequestBody PersonalRefreshRequest request, HttpServletRequest httpRequest) {
        PersonalAuthResponse response = personalAuthService.refresh(
                request.refreshToken(), httpRequest.getHeader(HttpHeaders.USER_AGENT), ClientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Sign out", description = "Revokes the supplied refresh token.")
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody PersonalLogoutRequest request) {
        personalAuthService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "Current account", description = "Returns the signed-in account's profile.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PersonalAccountResponse>> me(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalAuthService.me(principal)));
    }
}
