package com.project.klare_server.settings.web;

import com.project.klare_server.auth.dto.MessageResponse;
import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.settings.dto.ChangePasswordRequest;
import com.project.klare_server.settings.dto.ModeResponse;
import com.project.klare_server.settings.dto.UpdateModeRequest;
import com.project.klare_server.settings.dto.SettingsResponse;
import com.project.klare_server.settings.dto.UpdateCompanyProfileRequest;
import com.project.klare_server.settings.dto.UpdatePayrollAutomationRequest;
import com.project.klare_server.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "Settings", description = "Payroll automation, company profile and security")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Operation(summary = "Get the company settings (payroll automation and company profile)")
    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getSettings(principal.id())));
    }

    @Operation(summary = "Update payroll automation settings")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PutMapping("/payroll-automation")
    public ResponseEntity<ApiResponse<SettingsResponse>> updatePayrollAutomation(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdatePayrollAutomationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                settingsService.updatePayrollAutomation(principal.companyId(), request)));
    }

    @Operation(summary = "Update company profile and admin email")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PutMapping("/company-profile")
    public ResponseEntity<ApiResponse<SettingsResponse>> updateCompanyProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateCompanyProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                settingsService.updateCompanyProfile(principal.id(), request)));
    }

    @Operation(summary = "Switch between Live and Sandbox mode",
            description = "Sandbox simulates Moolre (instant funding and payouts, no real money) for testing. "
                    + "Live and Sandbox keep separate wallet balances.")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PutMapping("/mode")
    public ResponseEntity<ApiResponse<ModeResponse>> setMode(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateModeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.setMode(principal.companyId(), request.live())));
    }

    @Operation(summary = "Change the signed-in administrator's password",
            description = "Verifies the current password, sets the new one, and signs out all other sessions.")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        settingsService.changePassword(principal.id(), request);
        return ResponseEntity.ok(ApiResponse.ok(
                new MessageResponse("Your password has been changed. Please sign in again on your other devices.")));
    }
}
