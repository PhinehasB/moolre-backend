package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.personal.dto.PersonalHomeResponse;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal")
@Tag(name = "Personal Home", description = "Home dashboard for the Klare mobile app")
public class PersonalHomeController {

    private final PersonalHomeService personalHomeService;

    public PersonalHomeController(PersonalHomeService personalHomeService) {
        this.personalHomeService = personalHomeService;
    }

    @Operation(
            summary = "Home dashboard",
            description = "Returns the signed-in account's free spendable balance, locked safe wallet, next salary "
                    + "(for employees), and this month's bills.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<PersonalHomeResponse>> home(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(personalHomeService.home(principal)));
    }
}
