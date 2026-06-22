package com.project.klare_server.wallet.web;

import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.wallet.dto.FundWalletRequest;
import com.project.klare_server.wallet.dto.FundingResponse;
import com.project.klare_server.wallet.dto.SubmitFundingOtpRequest;
import com.project.klare_server.wallet.dto.WalletResponse;
import com.project.klare_server.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
@Tag(name = "Wallet", description = "Company wallet balance and top up")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(summary = "Get the wallet balance and recent top ups")
    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getWallet(principal.id())));
    }

    @Operation(summary = "Top up the wallet by charging a mobile money number (sends a USSD approval prompt)")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping("/fund")
    public ResponseEntity<ApiResponse<FundingResponse>> fund(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody FundWalletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.fund(principal.companyId(), request)));
    }

    @Operation(summary = "Submit the OTP for a funding request that requires verification")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping("/fund/{externalRef}/otp")
    public ResponseEntity<ApiResponse<FundingResponse>> submitOtp(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String externalRef,
            @Valid @RequestBody SubmitFundingOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                walletService.submitOtp(principal.companyId(), externalRef, request.otpcode())));
    }

    @Operation(summary = "Check and finalize a funding request's status")
    @GetMapping("/fund/{externalRef}/status")
    public ResponseEntity<ApiResponse<FundingResponse>> status(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String externalRef) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.checkStatus(principal.companyId(), externalRef)));
    }
}
