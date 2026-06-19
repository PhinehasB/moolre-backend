package com.project.klare_server.dashboard.web;

import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.dashboard.dto.DashboardSummaryResponse;
import com.project.klare_server.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Company dashboard overview")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get the company dashboard summary (wallet, next payroll, stats and team preview)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.summary(principal.id())));
    }
}
