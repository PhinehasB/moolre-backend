package com.project.klare_server.reports.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.ResourceNotFoundException;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.payroll.domain.PayrollItem;
import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import com.project.klare_server.payroll.repository.PayrollItemRepository;
import com.project.klare_server.payroll.repository.PayrollRunRepository;
import com.project.klare_server.reports.dto.ReportsOverviewResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportsService {

    private final BusinessUserRepository businessUserRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final PdfReports pdfReports;

    public ReportsService(
            BusinessUserRepository businessUserRepository,
            PayrollRunRepository payrollRunRepository,
            PayrollItemRepository payrollItemRepository,
            PdfReports pdfReports) {
        this.businessUserRepository = businessUserRepository;
        this.payrollRunRepository = payrollRunRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.pdfReports = pdfReports;
    }

    @Transactional(readOnly = true)
    public ReportsOverviewResponse overview(UUID userId) {
        Company company = loadCompany(userId);
        int year = currentYear();
        List<PayrollRun> runs = payrollRunRepository
                .findByCompanyIdAndStatusAndPeriodYearOrderByPeriodMonthDesc(company.getId(), PayrollRunStatus.COMPLETED, year);

        BigDecimal totalPaid = runs.stream().map(PayrollRun::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long employeesPaid = payrollItemRepository.countDistinctPaidEmployees(company.getId(), year);

        ReportsOverviewResponse.Stats stats = new ReportsOverviewResponse.Stats(
                year, totalPaid, runs.size(), employeesPaid, runs.size());

        List<ReportsOverviewResponse.AvailableReport> reports = new ArrayList<>();
        for (PayrollRun run : runs) {
            reports.add(new ReportsOverviewResponse.AvailableReport(
                    run.getId().toString(),
                    "PAYROLL_RUN",
                    "Payroll run report",
                    monthLabel(run.getPeriodMonth()) + " " + run.getPeriodYear(),
                    run.getEmployeeCount() + " employees",
                    List.of("CSV", "PDF")));
        }
        reports.add(new ReportsOverviewResponse.AvailableReport(
                "tax-summary", "TAX_SUMMARY", "Annual tax summary", year + " YTD", "all runs", List.of("PDF")));

        return new ReportsOverviewResponse(stats, reports);
    }

    @Transactional(readOnly = true)
    public ReportFile payrollRunReport(UUID userId, UUID runId, String format) {
        Company company = loadCompany(userId);
        PayrollRun run = payrollRunRepository.findByIdAndCompanyId(runId, company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found"));
        List<PayrollItem> items = payrollItemRepository.findByPayrollRunIdOrderByEmployeeNameAsc(runId);
        String period = monthLabel(run.getPeriodMonth()) + " " + run.getPeriodYear();
        String base = "payroll-" + run.getPeriodYear() + "-" + String.format("%02d", run.getPeriodMonth());

        if (isPdf(format)) {
            return new ReportFile(base + ".pdf",
                    pdfReports.payrollRunPdf(company.getName(), period, run, items), "application/pdf");
        }
        StringBuilder csv = new StringBuilder("employee,amount,status\n");
        for (PayrollItem item : items) {
            csv.append('"').append(item.getEmployeeName().replace("\"", "\"\"")).append('"')
                    .append(',').append(item.getAmount().toPlainString())
                    .append(',').append(item.getStatus().name())
                    .append('\n');
        }
        return new ReportFile(base + ".csv", csv.toString().getBytes(StandardCharsets.UTF_8), "text/csv");
    }

    @Transactional(readOnly = true)
    public ReportFile taxSummary(UUID userId, String format) {
        if (!isPdf(format)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The tax summary is only available as PDF");
        }
        Company company = loadCompany(userId);
        int year = currentYear();
        List<PayrollRun> runs = payrollRunRepository
                .findByCompanyIdAndStatusAndPeriodYearOrderByPeriodMonthDesc(company.getId(), PayrollRunStatus.COMPLETED, year);
        return new ReportFile("tax-summary-" + year + ".pdf",
                pdfReports.taxSummaryPdf(company.getName(), year, runs), "application/pdf");
    }

    private Company loadCompany(UUID userId) {
        BusinessUser user = businessUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return user.getCompany();
    }

    private boolean isPdf(String format) {
        return "pdf".equalsIgnoreCase(format);
    }

    private int currentYear() {
        return java.time.LocalDate.now(ZoneOffset.UTC).getYear();
    }

    private String monthLabel(int month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public record ReportFile(String filename, byte[] content, String contentType) {
    }
}
