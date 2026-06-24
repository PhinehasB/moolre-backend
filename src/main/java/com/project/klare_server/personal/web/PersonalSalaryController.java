package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.personal.dto.PendingSalaryResponse;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalSalaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal/salary")
@Tag(name = "Personal Salary", description = "Salary arrival updates for the mobile app")
public class PersonalSalaryController {

    private final PersonalSalaryService personalSalaryService;

    public PersonalSalaryController(PersonalSalaryService personalSalaryService) {
        this.personalSalaryService = personalSalaryService;
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
}
