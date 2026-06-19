package com.project.klare_server.employee.web;

import com.project.klare_server.auth.security.AuthenticatedUser;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.common.web.PageResponse;
import com.project.klare_server.employee.domain.EmployeeStatus;
import com.project.klare_server.employee.dto.CreateEmployeeRequest;
import com.project.klare_server.employee.dto.EmployeeResponse;
import com.project.klare_server.employee.dto.EmployeeStatsResponse;
import com.project.klare_server.employee.dto.ImportResultResponse;
import com.project.klare_server.employee.dto.UpdateEmployeeRequest;
import com.project.klare_server.employee.service.EmployeeImportService;
import com.project.klare_server.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Manage the company's team")
public class EmployeeController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String CSV_TEMPLATE = "name,email,phone,salary\nKwame Essien,kwame@techcorp.com,+233241112233,5000\n";

    private final EmployeeService employeeService;
    private final EmployeeImportService employeeImportService;

    public EmployeeController(EmployeeService employeeService, EmployeeImportService employeeImportService) {
        this.employeeService = employeeService;
        this.employeeImportService = employeeImportService;
    }

    @Operation(summary = "Add an employee to the company")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'HR')")
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = employeeService.create(principal.companyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(summary = "List the company's employees with search and status filtering")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(employeeService.list(principal.companyId(), q, status, pageable)));
    }

    @Operation(summary = "Get the team headcount and per-status counts")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<EmployeeStatsResponse>> stats(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.stats(principal.companyId())));
    }

    @Operation(summary = "Get a single employee")
    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> get(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.get(principal.companyId(), employeeId)));
    }

    @Operation(summary = "Update an employee")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'HR')")
    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.update(principal.companyId(), employeeId, request)));
    }

    @Operation(summary = "Bulk import employees from a CSV or Excel file")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'HR')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResultResponse>> importEmployees(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean sendInvitations) {
        ImportResultResponse result = employeeImportService.importEmployees(principal.companyId(), file, sendInvitations);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Download the CSV import template")
    @GetMapping("/import/template")
    public ResponseEntity<Resource> importTemplate() {
        ByteArrayResource resource = new ByteArrayResource(CSV_TEMPLATE.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"klare-employee-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
