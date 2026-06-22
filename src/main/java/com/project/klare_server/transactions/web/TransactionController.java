package com.project.klare_server.transactions.web;

import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.common.web.PageResponse;
import com.project.klare_server.transactions.dto.TransactionFilter;
import com.project.klare_server.transactions.service.TransactionService;
import com.project.klare_server.wallet.dto.LedgerEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Every movement on the company wallet")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "List wallet transactions with filter (ALL/INFLOWS/PAYOUTS/FAILED) and search")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LedgerEntry>>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(value = "filter", defaultValue = "ALL") TransactionFilter filter,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                transactionService.list(principal.id(), filter, query, page, size)));
    }

    @Operation(summary = "Export the filtered transactions as CSV")
    @GetMapping("/export")
    public ResponseEntity<Resource> export(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(value = "filter", defaultValue = "ALL") TransactionFilter filter,
            @RequestParam(value = "q", required = false) String query) {
        TransactionService.ReportFile file = transactionService.export(principal.id(), filter, query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new ByteArrayResource(file.content()));
    }
}
