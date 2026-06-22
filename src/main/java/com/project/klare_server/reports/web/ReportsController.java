package com.project.klare_server.reports.web;

import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.reports.dto.ReportsOverviewResponse;
import com.project.klare_server.reports.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Export payroll records for accounting")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @Operation(summary = "Get the reports overview (stats and downloadable reports)")
    @GetMapping
    public ResponseEntity<ApiResponse<ReportsOverviewResponse>> overview(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.overview(principal.id())));
    }

    @Operation(summary = "Download a payroll run report as CSV or PDF")
    @GetMapping("/payroll-runs/{runId}")
    public ResponseEntity<Resource> payrollRunReport(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID runId,
            @RequestParam(value = "format", defaultValue = "csv") String format) {
        return download(reportsService.payrollRunReport(principal.id(), runId, format));
    }

    @Operation(summary = "Download the annual tax summary as PDF")
    @GetMapping("/tax-summary")
    public ResponseEntity<Resource> taxSummary(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        return download(reportsService.taxSummary(principal.id(), format));
    }

    private ResponseEntity<Resource> download(ReportsService.ReportFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(new ByteArrayResource(file.content()));
    }
}
