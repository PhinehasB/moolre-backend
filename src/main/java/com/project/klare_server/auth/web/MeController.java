package com.project.klare_server.auth.web;

import com.project.klare_server.auth.dto.MeResponse;
import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.auth.service.AccountService;
import com.project.klare_server.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Current Account", description = "The signed-in administrator and their company")
public class MeController {

    private final AccountService accountService;

    public MeController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Get the current administrator and company")
    @GetMapping
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.currentUser(principal.id())));
    }
}
