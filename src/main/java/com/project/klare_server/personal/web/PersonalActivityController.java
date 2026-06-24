package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.personal.dto.ActivityItemResponse;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal")
@Tag(name = "Personal Activity", description = "Transaction history for the mobile app")
public class PersonalActivityController {

    private final PersonalActivityService personalActivityService;

    public PersonalActivityController(PersonalActivityService personalActivityService) {
        this.personalActivityService = personalActivityService;
    }

    @Operation(
            summary = "Activity feed",
            description = "Returns the account's recent transactions (sends, salary, sweeps) newest first.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<ActivityItemResponse>>> transactions(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalActivityService.list(principal)));
    }
}
