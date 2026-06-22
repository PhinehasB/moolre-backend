package com.project.klare_server.reports.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportsOverviewResponse(Stats stats, List<AvailableReport> reports) {

    public record Stats(
            int year,
            BigDecimal totalPaid,
            long payrollRuns,
            long employeesPaid,
            long reportsGenerated) {
    }

    public record AvailableReport(
            String id,
            String kind,
            String title,
            String period,
            String records,
            List<String> formats) {
    }
}
