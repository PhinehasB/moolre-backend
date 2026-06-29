package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalSalaryEvent;
import com.project.klare_server.personal.dto.DepositSalaryRequest;
import com.project.klare_server.personal.dto.PendingSalaryResponse;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalPaymentService;
import com.project.klare_server.personal.service.PersonalSalaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal/salary")
@Tag(name = "Personal Salary", description = "Salary arrival updates for the mobile app")
public class PersonalSalaryController {

    private final PersonalSalaryService personalSalaryService;
    private final PersonalPaymentService personalPaymentService;

    public PersonalSalaryController(
            PersonalSalaryService personalSalaryService, PersonalPaymentService personalPaymentService) {
        this.personalSalaryService = personalSalaryService;
        this.personalPaymentService = personalPaymentService;
    }

    @Operation(
            summary = "Pending salary update",
            description = "Returns the most recent salary arrival the user has not yet acknowledged, or null. "
                    + "The app shows the 'Salary received' sheet when this is present.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<PendingSalaryResponse>> pending(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalSalaryService.pending(principal)));
    }

    @Operation(summary = "Acknowledge a salary update", description = "Dismisses the 'Salary received' sheet for this user.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<Void>> acknowledge(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal, @PathVariable UUID id) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        personalSalaryService.acknowledge(principal, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(
            summary = "Record a salary payment",
            description = "Credits the employee's wallet (spendable + safe split), records the salary and sweep, and "
                    + "sends email, SMS and push notifications. Triggers the in-app 'Salary received' sheet.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PendingSalaryResponse>> deposit(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @Valid @RequestBody(required = false) DepositSalaryRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (principal.accountType() != PersonalAccountType.EMPLOYEE) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only employee accounts receive a salary.");
        }
        PersonalSalaryEvent event = personalPaymentService.recordSalaryPayment(
                principal.id(), request != null ? request.amount() : null);
        return ResponseEntity.ok(ApiResponse.ok(PendingSalaryResponse.from(event, "GHS")));
    }
}
