package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.personal.dto.TransferRequest;
import com.project.klare_server.personal.dto.TransferResponse;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal/transfers")
@Tag(name = "Personal Transfers", description = "Send money from spendable cash to mobile money or bank")
public class PersonalTransferController {

    private final PersonalTransferService personalTransferService;

    public PersonalTransferController(PersonalTransferService personalTransferService) {
        this.personalTransferService = personalTransferService;
    }

    @Operation(
            summary = "Send money",
            description = "Sends from the account's free spendable balance. Adds a 1% E-Levy and 0.5% Klare fee, checks "
                    + "the balance covers the total, debits the wallet, and records the transaction.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> send(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @Valid @RequestBody TransferRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(personalTransferService.send(principal, request)));
    }
}
