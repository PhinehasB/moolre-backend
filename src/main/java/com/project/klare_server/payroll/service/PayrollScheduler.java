package com.project.klare_server.payroll.service;

import com.project.klare_server.auth.domain.BusinessUserRole;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.domain.CompanyStatus;
import com.project.klare_server.company.repository.CompanyRepository;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.domain.EmployeeStatus;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.notification.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PayrollScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayrollScheduler.class);
    private static final DateTimeFormatter PAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final int ESTIMATE_LEAD_DAYS = 5;
    private static final int PAYDAY_REMINDER_LEAD_DAYS = 2;

    private final CompanyRepository companyRepository;
    private final BusinessUserRepository businessUserRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollService payrollService;
    private final NotificationService notificationService;

    public PayrollScheduler(
            CompanyRepository companyRepository,
            BusinessUserRepository businessUserRepository,
            EmployeeRepository employeeRepository,
            PayrollService payrollService,
            NotificationService notificationService) {
        this.companyRepository = companyRepository;
        this.businessUserRepository = businessUserRepository;
        this.employeeRepository = employeeRepository;
        this.payrollService = payrollService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${klare.payroll.auto-run-cron:0 0 9 * * *}", zone = "UTC")
    public void tick() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (Company company : companyRepository.findByAutoPayrollEnabledTrue()) {
            if (company.getStatus() != CompanyStatus.ACTIVE) {
                continue;
            }
            try {
                LocalDate payDate = nextPayrollDate(company.getPayrollDayOfMonth(), today);
                long daysUntil = ChronoUnit.DAYS.between(today, payDate);
                if (daysUntil == 0) {
                    runPayroll(company);
                } else if (daysUntil == ESTIMATE_LEAD_DAYS && company.isEmailEstimateBeforeRun()) {
                    sendEstimate(company, payDate);
                } else if (daysUntil == PAYDAY_REMINDER_LEAD_DAYS && company.isNotifyEmployeesBeforePayday()) {
                    notifyEmployees(company, payDate);
                }
            } catch (Exception ex) {
                log.error("Automatic payroll tick failed for company={} error={}", company.getId(), ex.toString());
            }
        }
    }

    private void runPayroll(Company company) {
        businessUserRepository.findFirstByCompanyIdAndRole(company.getId(), BusinessUserRole.OWNER)
                .ifPresent(owner -> {
                    PayrollService.AutoRunResult result = payrollService.tryAutoRun(company.getId(), owner.getId());
                    log.info("Automatic payroll for company={} outcome={}", company.getName(), result.outcome());
                });
    }

    private void sendEstimate(Company company, LocalDate payDate) {
        long active = employeeRepository.countByCompanyIdAndStatus(company.getId(), EmployeeStatus.ACTIVE);
        if (active == 0) {
            return;
        }
        BigDecimal total = employeeRepository.sumMonthlySalaryByStatus(company.getId(), EmployeeStatus.ACTIVE);
        businessUserRepository.findFirstByCompanyIdAndRole(company.getId(), BusinessUserRole.OWNER)
                .ifPresent(owner -> notificationService.payrollEstimate(
                        owner.getEmail(), owner.getPhone(), owner.getFirstName(), company.getName(),
                        PAY_DATE.format(payDate), total.toPlainString(), (int) active));
    }

    private void notifyEmployees(Company company, LocalDate payDate) {
        for (Employee employee : employeeRepository.findByCompanyIdAndStatus(company.getId(), EmployeeStatus.ACTIVE)) {
            notificationService.paydayReminder(employee.getEmail(), employee.getPhone(),
                    employee.getFirstName(), company.getName(), PAY_DATE.format(payDate));
        }
    }

    private LocalDate nextPayrollDate(int payrollDayOfMonth, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        LocalDate candidate = current.atDay(Math.min(payrollDayOfMonth, current.lengthOfMonth()));
        if (!candidate.isBefore(today)) {
            return candidate;
        }
        YearMonth next = current.plusMonths(1);
        return next.atDay(Math.min(payrollDayOfMonth, next.lengthOfMonth()));
    }
}
