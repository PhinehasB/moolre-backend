package com.project.klare_server.payroll.web;

import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.payroll.dto.AutoRunResponse;
import com.project.klare_server.payroll.dto.ConfirmPayrollRequest;
import com.project.klare_server.payroll.dto.PayrollInitiationResponse;
import com.project.klare_server.payroll.dto.PayrollOverviewResponse;
import com.project.klare_server.payroll.dto.PayrollRunResponse;
import com.project.klare_server.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/payroll")
@Tag(name = "Payroll", description = "Run, schedule and review payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @Operation(summary = "Get the payroll overview (run summary, schedule and history)")
    @GetMapping
    public ResponseEntity<ApiResponse<PayrollOverviewResponse>> overview(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.overview(principal.companyId())));
    }

    @Operation(summary = "Initiate a payroll run and send the SMS confirmation code")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping("/runs/initiate")
    public ResponseEntity<ApiResponse<PayrollInitiationResponse>> initiate(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.initiate(principal.companyId(), principal.id())));
    }

    @Operation(summary = "Confirm a payroll run with the 6-digit code and execute payment")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping("/runs/{runId}/confirm")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> confirm(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID runId,
            @Valid @RequestBody ConfirmPayrollRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.confirm(principal.companyId(), runId, request.code())));
    }

    @Operation(
            summary = "Run automatic payroll now (no SMS code)",
            description = "Runs payroll hands-free for the company, skipping the SMS confirmation. "
                    + "Requires automatic payroll to be enabled in Settings (the standing authorization). "
                    + "This is also what the daily scheduler invokes on each company's pay date.")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping("/runs/auto-run")
    public ResponseEntity<ApiResponse<AutoRunResponse>> autoRun(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                AutoRunResponse.from(payrollService.tryAutoRun(principal.companyId(), principal.id()))));
    }

    @Operation(summary = "Reconcile a processing payroll run by polling pending transfers")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping("/runs/{runId}/reconcile")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> reconcile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID runId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.reconcile(principal.companyId(), runId)));
    }

    @Operation(summary = "Download a payroll run report as CSV")
    @GetMapping("/runs/{runId}/report")
    public ResponseEntity<Resource> report(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID runId) {
        PayrollService.ReportFile file = payrollService.report(principal.companyId(), runId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new ByteArrayResource(file.content()));
    }
}
