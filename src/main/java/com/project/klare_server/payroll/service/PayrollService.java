package com.project.klare_server.payroll.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.ResourceNotFoundException;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.security.TokenHasher;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.company.domain.CompanyWallet;
import com.project.klare_server.company.repository.CompanyRepository;
import com.project.klare_server.company.repository.CompanyWalletRepository;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.domain.EmployeeStatus;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.moolre.GhanaMobileMoney;
import com.project.klare_server.moolre.MoolreClient;
import com.project.klare_server.moolre.MoolreException;
import com.project.klare_server.payroll.domain.PayrollItem;
import com.project.klare_server.payroll.domain.PayrollItemStatus;
import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import com.project.klare_server.payroll.dto.PayrollInitiationResponse;
import com.project.klare_server.payroll.dto.PayrollOverviewResponse;
import com.project.klare_server.payroll.dto.PayrollRunResponse;
import com.project.klare_server.payroll.notification.SmsService;
import com.project.klare_server.payroll.repository.PayrollItemRepository;
import com.project.klare_server.payroll.repository.PayrollRunRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private static final int NOTIFY_LEAD_DAYS = 2;

    private final CompanyRepository companyRepository;
    private final CompanyWalletRepository companyWalletRepository;
    private final EmployeeRepository employeeRepository;
    private final BusinessUserRepository businessUserRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final PayrollConfirmationAttemptService attemptService;
    private final SmsService smsService;
    private final MoolreClient moolreClient;
    private final String pepper;
    private final SecureRandom secureRandom = new SecureRandom();

    public PayrollService(
            CompanyRepository companyRepository,
            CompanyWalletRepository companyWalletRepository,
            EmployeeRepository employeeRepository,
            BusinessUserRepository businessUserRepository,
            PayrollRunRepository payrollRunRepository,
            PayrollItemRepository payrollItemRepository,
            PayrollConfirmationAttemptService attemptService,
            SmsService smsService,
            MoolreClient moolreClient,
            SecurityProperties securityProperties) {
        this.companyRepository = companyRepository;
        this.companyWalletRepository = companyWalletRepository;
        this.employeeRepository = employeeRepository;
        this.businessUserRepository = businessUserRepository;
        this.payrollRunRepository = payrollRunRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.attemptService = attemptService;
        this.smsService = smsService;
        this.moolreClient = moolreClient;
        this.pepper = securityProperties.refreshToken().pepper();
    }

    @Transactional(readOnly = true)
    public PayrollOverviewResponse overview(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        int activeEmployees = (int) employeeRepository.countByCompanyIdAndStatus(companyId, EmployeeStatus.ACTIVE);
        BigDecimal total = employeeRepository.sumMonthlySalaryByStatus(companyId, EmployeeStatus.ACTIVE);
        BigDecimal balance = walletBalance(companyId);
        boolean covers = balance.compareTo(total) >= 0;
        BigDecimal shortfall = covers ? BigDecimal.ZERO : total.subtract(balance);

        PayrollOverviewResponse.RunSummary run = new PayrollOverviewResponse.RunSummary(
                activeEmployees, total, balance, coveragePercent(balance, total), covers, shortfall);

        PayrollOverviewResponse.Schedule schedule = new PayrollOverviewResponse.Schedule(
                company.isAutoPayrollEnabled(),
                company.getPayrollDayOfMonth(),
                company.isNotifyEmployeesBeforePayday(),
                NOTIFY_LEAD_DAYS,
                company.isAutoPayrollEnabled() ? "ACTIVE" : "PAUSED");

        List<PayrollRunResponse> history = payrollRunRepository
                .findTop12ByCompanyIdAndStatusOrderByCompletedAtDesc(companyId, PayrollRunStatus.COMPLETED)
                .stream().map(PayrollRunResponse::from).toList();

        return new PayrollOverviewResponse(run, schedule, history);
    }

    @Transactional
    public PayrollInitiationResponse initiate(UUID companyId, UUID userId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        BusinessUser admin = businessUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        List<Employee> activeEmployees = employeeRepository.findByCompanyIdAndStatus(companyId, EmployeeStatus.ACTIVE);
        if (activeEmployees.isEmpty()) {
            throw new ApiException(ErrorCode.CONFLICT, "There are no active employees to pay");
        }
        BigDecimal total = activeEmployees.stream()
                .map(Employee::getMonthlySalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CompanyWallet wallet = companyWalletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "No wallet is set up for this company"));
        if (wallet.getBalance().compareTo(total) < 0) {
            throw new ApiException(ErrorCode.CONFLICT, "Your wallet does not cover this payroll. Top up first.");
        }

        payrollRunRepository.findByCompanyIdAndStatus(companyId, PayrollRunStatus.PENDING_CONFIRMATION)
                .forEach(existing -> existing.setStatus(PayrollRunStatus.CANCELLED));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String code = generateCode();

        PayrollRun run = new PayrollRun();
        run.setCompany(company);
        run.setPeriodYear(today.getYear());
        run.setPeriodMonth(today.getMonthValue());
        run.setStatus(PayrollRunStatus.PENDING_CONFIRMATION);
        run.setEmployeeCount(activeEmployees.size());
        run.setTotalAmount(total);
        run.setInitiatedByUserId(userId);
        run.setConfirmationCodeHash(TokenHasher.sha256(pepper, code));
        run.setConfirmationPhoneLast4(last4(admin.getPhone()));
        run.setConfirmationExpiresAt(Instant.now().plus(CODE_TTL));
        payrollRunRepository.save(run);

        for (Employee employee : activeEmployees) {
            PayrollItem item = new PayrollItem();
            item.setPayrollRun(run);
            item.setEmployee(employee);
            item.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
            item.setAmount(employee.getMonthlySalary());
            item.setStatus(PayrollItemStatus.PENDING);
            item.setExternalRef("kp_" + UUID.randomUUID().toString().replace("-", ""));
            payrollItemRepository.save(item);
        }

        smsService.sendPayrollConfirmationCode(admin.getPhone(), code);

        return new PayrollInitiationResponse(
                run.getId(), activeEmployees.size(), total, wallet.getBalance(),
                maskPhone(admin.getPhone()), run.getConfirmationExpiresAt());
    }

    @Transactional
    public PayrollRunResponse confirm(UUID companyId, UUID runId, String code) {
        PayrollRun run = payrollRunRepository.findByIdAndCompanyId(runId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found"));
        if (run.getStatus() != PayrollRunStatus.PENDING_CONFIRMATION) {
            throw new ApiException(ErrorCode.CONFLICT, "This payroll can no longer be confirmed");
        }
        Instant now = Instant.now();
        if (run.getConfirmationExpiresAt() == null || run.getConfirmationExpiresAt().isBefore(now)) {
            run.setStatus(PayrollRunStatus.EXPIRED);
            throw new ApiException(ErrorCode.CONFLICT, "The confirmation code has expired. Please start a new payroll.");
        }

        String hash = TokenHasher.sha256(pepper, code);
        if (!hash.equals(run.getConfirmationCodeHash())) {
            boolean locked = attemptService.recordFailedAttempt(runId, MAX_ATTEMPTS);
            if (locked) {
                throw new ApiException(ErrorCode.CONFLICT, "Too many incorrect codes. This payroll was cancelled.");
            }
            throw new ApiException(ErrorCode.INVALID_TOKEN, "Incorrect confirmation code");
        }

        CompanyWallet wallet = companyWalletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "No wallet is set up for this company"));
        if (wallet.getBalance().compareTo(run.getTotalAmount()) < 0) {
            throw new ApiException(ErrorCode.CONFLICT, "Your wallet no longer covers this payroll");
        }
        run.setConfirmationCodeHash(null);

        List<PayrollItem> items = payrollItemRepository.findByPayrollRunIdOrderByEmployeeNameAsc(runId);
        int paid = 0;
        int failed = 0;
        int pending = 0;
        BigDecimal committed = BigDecimal.ZERO;

        for (PayrollItem item : items) {
            PayrollItemStatus result = disburse(item);
            item.setStatus(result);
            switch (result) {
                case PAID -> {
                    paid++;
                    committed = committed.add(item.getAmount());
                }
                case PENDING -> {
                    pending++;
                    committed = committed.add(item.getAmount());
                }
                case FAILED -> failed++;
            }
        }

        wallet.setBalance(wallet.getBalance().subtract(committed));
        run.setSuccessCount(paid);
        run.setFailureCount(failed);
        if (pending > 0) {
            run.setStatus(PayrollRunStatus.PROCESSING);
        } else {
            run.setStatus(PayrollRunStatus.COMPLETED);
            run.setCompletedAt(now);
        }

        return PayrollRunResponse.from(run);
    }

    private PayrollItemStatus disburse(PayrollItem item) {
        String channel;
        String receiver;
        try {
            channel = GhanaMobileMoney.resolveChannel(item.getEmployee().getPhone());
            receiver = GhanaMobileMoney.normalize(item.getEmployee().getPhone());
        } catch (MoolreException ex) {
            item.setFailureReason(ex.getMessage());
            return PayrollItemStatus.FAILED;
        }

        try {
            MoolreClient.TransferResult result = moolreClient.transfer(
                    channel, item.getAmount(), receiver, item.getExternalRef(), "Salary payment");
            item.setTransactionId(result.transactionId());
            Integer tx = result.txstatus();
            if (tx != null && tx == 1) {
                return PayrollItemStatus.PAID;
            }
            if (tx != null && (tx == 0 || tx == 3)) {
                return PayrollItemStatus.PENDING;
            }
            if (tx != null && tx == 2) {
                item.setFailureReason(result.message() != null ? result.message() : result.code());
                return PayrollItemStatus.FAILED;
            }
            item.setFailureReason(result.message() != null ? result.message() : result.code());
            return PayrollItemStatus.FAILED;
        } catch (MoolreException ex) {
            item.setFailureReason(ex.getMessage());
            return PayrollItemStatus.PENDING;
        }
    }

    @Transactional
    public PayrollRunResponse reconcile(UUID companyId, UUID runId) {
        PayrollRun run = payrollRunRepository.findByIdAndCompanyId(runId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found"));
        if (run.getStatus() != PayrollRunStatus.PROCESSING) {
            return PayrollRunResponse.from(run);
        }
        CompanyWallet wallet = companyWalletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "No wallet is set up for this company"));

        int paid = 0;
        int failed = 0;
        int pending = 0;
        for (PayrollItem item : payrollItemRepository.findByPayrollRunIdOrderByEmployeeNameAsc(runId)) {
            if (item.getStatus() == PayrollItemStatus.PENDING) {
                try {
                    MoolreClient.TransferStatusResult status = moolreClient.transferStatus(item.getExternalRef());
                    Integer tx = status.txstatus();
                    if (tx != null && tx == 1) {
                        item.setStatus(PayrollItemStatus.PAID);
                        item.setTransactionId(status.transactionId());
                    } else if (tx != null && tx == 2) {
                        item.setStatus(PayrollItemStatus.FAILED);
                        item.setFailureReason("Transfer failed");
                        wallet.setBalance(wallet.getBalance().add(item.getAmount()));
                    }
                } catch (MoolreException ignored) {
                    // leave pending and try again on the next reconcile
                }
            }
            switch (item.getStatus()) {
                case PAID -> paid++;
                case FAILED -> failed++;
                case PENDING -> pending++;
            }
        }

        run.setSuccessCount(paid);
        run.setFailureCount(failed);
        if (pending == 0) {
            run.setStatus(PayrollRunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
        }
        return PayrollRunResponse.from(run);
    }

    @Transactional(readOnly = true)
    public ReportFile report(UUID companyId, UUID runId) {
        PayrollRun run = payrollRunRepository.findByIdAndCompanyId(runId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found"));
        List<PayrollItem> items = payrollItemRepository.findByPayrollRunIdOrderByEmployeeNameAsc(runId);

        StringBuilder csv = new StringBuilder("employee,amount,status\n");
        for (PayrollItem item : items) {
            csv.append('"').append(item.getEmployeeName().replace("\"", "\"\"")).append('"')
                    .append(',').append(item.getAmount().toPlainString())
                    .append(',').append(item.getStatus().name())
                    .append('\n');
        }
        String filename = "payroll-" + run.getPeriodYear() + "-"
                + String.format("%02d", run.getPeriodMonth()) + ".csv";
        return new ReportFile(filename, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private BigDecimal walletBalance(UUID companyId) {
        return companyWalletRepository.findByCompanyId(companyId)
                .map(CompanyWallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    private int coveragePercent(BigDecimal balance, BigDecimal total) {
        if (total.signum() <= 0) {
            return 100;
        }
        int percent = balance.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.FLOOR)
                .intValue();
        return Math.min(percent, 100);
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String last4(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
    }

    private String maskPhone(String phone) {
        return "****" + last4(phone);
    }

    public record ReportFile(String filename, byte[] content) {
    }
}
