package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.personal.dto.BillPaymentResponse;
import com.project.klare_server.personal.dto.CreateObligationRequest;
import com.project.klare_server.personal.dto.PersonalBillsResponse;
import com.project.klare_server.personal.dto.UpdateObligationRequest;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalBillPaymentService;
import com.project.klare_server.personal.service.PersonalBillsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal")
@Tag(name = "Personal Bills", description = "Bills and expenses auto-swept each payday")
public class PersonalBillsController {

    private final PersonalBillsService personalBillsService;
    private final PersonalBillPaymentService personalBillPaymentService;

    public PersonalBillsController(
            PersonalBillsService personalBillsService, PersonalBillPaymentService personalBillPaymentService) {
        this.personalBillsService = personalBillsService;
        this.personalBillPaymentService = personalBillPaymentService;
    }

    @Operation(
            summary = "Bills and expenses",
            description = "Returns the monthly income split (locked for bills vs spendable) and every obligation, "
                    + "including paused ones.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/bills")
    public ResponseEntity<ApiResponse<PersonalBillsResponse>> bills(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalBillsService.bills(principal)));
    }

    @Operation(
            summary = "Add an expense",
            description = "Creates a recurring obligation that Klare locks and auto-sweeps each payday.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/bills")
    public ResponseEntity<ApiResponse<PersonalBillsResponse.Bill>> create(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @Valid @RequestBody CreateObligationRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(personalBillsService.create(principal, request)));
    }

    @Operation(summary = "Update an expense", description = "Toggle it on/off or change its amount or name.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/bills/{id}")
    public ResponseEntity<ApiResponse<PersonalBillsResponse.Bill>> update(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateObligationRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalBillsService.update(principal, id, request)));
    }

    @Operation(summary = "Remove an expense", description = "Deletes an obligation so Klare no longer sets it aside.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/bills/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal, @PathVariable UUID id) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        personalBillsService.delete(principal, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(
            summary = "Let Klare pay the bills",
            description = "Disburses every active bill that has a mobile money destination to its recipient via Moolre, "
                    + "debiting the locked safe wallet.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/bills/pay")
    public ResponseEntity<ApiResponse<BillPaymentResponse>> pay(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalBillPaymentService.payActiveBills(principal)));
    }
}
