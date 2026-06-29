package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.personal.dto.FundWalletRequest;
import com.project.klare_server.personal.dto.FundingResponse;
import com.project.klare_server.personal.dto.SubmitFundingOtpRequest;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalFundingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal/wallet")
@Tag(name = "Personal Wallet", description = "Add money to the Klare wallet via mobile money")
public class PersonalWalletController {

    private final PersonalFundingService personalFundingService;

    public PersonalWalletController(PersonalFundingService personalFundingService) {
        this.personalFundingService = personalFundingService;
    }

    @Operation(
            summary = "Add money",
            description = "Starts a mobile money debit (Moolre). Depending on the network it either returns AWAITING_OTP "
                    + "(submit the OTP) or AWAITING_APPROVAL (approve the prompt on the phone), then poll the status.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/fund")
    public ResponseEntity<ApiResponse<FundingResponse>> fund(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @Valid @RequestBody FundWalletRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalFundingService.fund(principal, request)));
    }

    @Operation(summary = "Submit a top-up OTP", description = "Verifies the OTP and charges the mobile money number.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/fund/otp")
    public ResponseEntity<ApiResponse<FundingResponse>> submitOtp(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @Valid @RequestBody SubmitFundingOtpRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                personalFundingService.submitOtp(principal, request.externalRef(), request.otpcode())));
    }

    @Operation(summary = "Check top-up status", description = "Polls the payment status and credits the wallet once it succeeds.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/fund/{externalRef}/status")
    public ResponseEntity<ApiResponse<FundingResponse>> status(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal, @PathVariable String externalRef) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalFundingService.checkStatus(principal, externalRef)));
    }
}
