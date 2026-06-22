package com.project.klare_server.dashboard.dto;

import com.project.klare_server.employee.dto.EmployeeResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryResponse(
        Greeting greeting,
        Wallet wallet,
        NextPayroll nextPayroll,
        LastPayroll lastPayroll,
        Stats stats,
        List<EmployeeResponse> team) {

    public record Greeting(String firstName, String companyName) {
    }

    public record LastPayroll(BigDecimal amount, LocalDate date, int successRate, int employees) {
    }

    public record Wallet(BigDecimal balance, BigDecimal pending, String currency) {
    }

    public record NextPayroll(
            LocalDate date,
            long inDays,
            boolean autoEnabled,
            long activeEmployees,
            BigDecimal totalToPay,
            boolean walletCoversInFull,
            BigDecimal shortfall) {
    }

    public record Stats(
            long activeEmployees,
            long pendingOnboarding,
            long totalEmployees,
            long addedThisMonth,
            BigDecimal totalMonthlyPayroll) {
    }
}
