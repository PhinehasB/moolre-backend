package com.project.klare_server.dashboard.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.domain.CompanyWallet;
import com.project.klare_server.company.repository.CompanyWalletRepository;
import com.project.klare_server.dashboard.dto.DashboardSummaryResponse;
import com.project.klare_server.employee.dto.EmployeeResponse;
import com.project.klare_server.employee.domain.EmployeeStatus;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import com.project.klare_server.payroll.repository.PayrollRunRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final BusinessUserRepository businessUserRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyWalletRepository companyWalletRepository;
    private final PayrollRunRepository payrollRunRepository;

    public DashboardService(
            BusinessUserRepository businessUserRepository,
            EmployeeRepository employeeRepository,
            CompanyWalletRepository companyWalletRepository,
            PayrollRunRepository payrollRunRepository) {
        this.businessUserRepository = businessUserRepository;
        this.employeeRepository = employeeRepository;
        this.companyWalletRepository = companyWalletRepository;
        this.payrollRunRepository = payrollRunRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(UUID userId) {
        BusinessUser user = businessUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        Company company = user.getCompany();
        UUID companyId = company.getId();

        long activeEmployees = employeeRepository.countByCompanyIdAndStatus(companyId, EmployeeStatus.ACTIVE);
        long pendingOnboarding = employeeRepository.countByCompanyIdAndStatus(companyId, EmployeeStatus.PENDING);
        long totalEmployees = employeeRepository.countByCompanyId(companyId);
        long addedThisMonth = employeeRepository.countByCompanyIdAndCreatedAtGreaterThanEqual(companyId, startOfMonth());
        BigDecimal totalToPay = employeeRepository.sumMonthlySalaryByStatus(companyId, EmployeeStatus.ACTIVE);

        boolean live = company.isLiveMode();
        DashboardSummaryResponse.Wallet wallet = companyWalletRepository.findByCompanyId(companyId)
                .map(w -> toWallet(w, live))
                .orElseGet(() -> new DashboardSummaryResponse.Wallet(BigDecimal.ZERO, BigDecimal.ZERO, "GHS"));

        LocalDate payrollDate = nextPayrollDate(company.getPayrollDayOfMonth());
        boolean coversInFull = wallet.balance().compareTo(totalToPay) >= 0;
        BigDecimal shortfall = coversInFull ? BigDecimal.ZERO : totalToPay.subtract(wallet.balance());

        DashboardSummaryResponse.NextPayroll nextPayroll = new DashboardSummaryResponse.NextPayroll(
                payrollDate,
                ChronoUnit.DAYS.between(LocalDate.now(ZoneOffset.UTC), payrollDate),
                company.isAutoPayrollEnabled(),
                activeEmployees,
                totalToPay,
                coversInFull,
                shortfall);

        DashboardSummaryResponse.Stats stats = new DashboardSummaryResponse.Stats(
                activeEmployees, pendingOnboarding, totalEmployees, addedThisMonth, totalToPay);

        DashboardSummaryResponse.LastPayroll lastPayroll = payrollRunRepository
                .findTop12ByCompanyIdAndStatusAndLiveModeOrderByCompletedAtDesc(companyId, PayrollRunStatus.COMPLETED, live)
                .stream().findFirst()
                .map(this::toLastPayroll)
                .orElse(null);

        List<EmployeeResponse> team = employeeRepository.findTop5ByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(EmployeeResponse::from)
                .toList();

        return new DashboardSummaryResponse(
                new DashboardSummaryResponse.Greeting(user.getFirstName(), company.getName()),
                wallet,
                nextPayroll,
                lastPayroll,
                stats,
                team);
    }

    private DashboardSummaryResponse.LastPayroll toLastPayroll(PayrollRun run) {
        int successRate = run.getEmployeeCount() == 0 ? 0
                : Math.round(run.getSuccessCount() * 100f / run.getEmployeeCount());
        LocalDate date = (run.getCompletedAt() != null ? run.getCompletedAt() : run.getCreatedAt())
                .atZone(ZoneOffset.UTC).toLocalDate();
        return new DashboardSummaryResponse.LastPayroll(run.getTotalAmount(), date, successRate, run.getEmployeeCount());
    }

    private DashboardSummaryResponse.Wallet toWallet(CompanyWallet wallet, boolean live) {
        return new DashboardSummaryResponse.Wallet(
                wallet.activeBalance(live), live ? wallet.getPending() : BigDecimal.ZERO, wallet.getCurrency());
    }

    private Instant startOfMonth() {
        return YearMonth.now(ZoneOffset.UTC).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private LocalDate nextPayrollDate(int payrollDayOfMonth) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth current = YearMonth.from(today);
        LocalDate candidate = current.atDay(Math.min(payrollDayOfMonth, current.lengthOfMonth()));
        if (!candidate.isBefore(today)) {
            return candidate;
        }
        YearMonth next = current.plusMonths(1);
        return next.atDay(Math.min(payrollDayOfMonth, next.lengthOfMonth()));
    }
}
