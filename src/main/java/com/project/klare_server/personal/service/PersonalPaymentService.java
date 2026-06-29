package com.project.klare_server.personal.service;

import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalObligation;
import com.project.klare_server.personal.domain.PersonalSalaryEvent;
import com.project.klare_server.personal.domain.PersonalTransaction;
import com.project.klare_server.personal.domain.PersonalWallet;
import com.project.klare_server.personal.domain.TransactionStatus;
import com.project.klare_server.personal.domain.TransactionType;
import com.project.klare_server.personal.notification.PersonalNotificationService;
import com.project.klare_server.personal.repository.PersonalObligationRepository;
import com.project.klare_server.personal.repository.PersonalSalaryEventRepository;
import com.project.klare_server.personal.repository.PersonalTransactionRepository;
import com.project.klare_server.personal.repository.PersonalWalletRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalPaymentService {

    private final EmployeeRepository employeeRepository;
    private final PersonalWalletRepository walletRepository;
    private final PersonalObligationRepository obligationRepository;
    private final PersonalSalaryEventRepository salaryEventRepository;
    private final PersonalTransactionRepository transactionRepository;
    private final PersonalNotificationService notificationService;

    public PersonalPaymentService(
            EmployeeRepository employeeRepository,
            PersonalWalletRepository walletRepository,
            PersonalObligationRepository obligationRepository,
            PersonalSalaryEventRepository salaryEventRepository,
            PersonalTransactionRepository transactionRepository,
            PersonalNotificationService notificationService) {
        this.employeeRepository = employeeRepository;
        this.walletRepository = walletRepository;
        this.obligationRepository = obligationRepository;
        this.salaryEventRepository = salaryEventRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersonalSalaryEvent recordSalaryPayment(UUID employeeId, BigDecimal requestedAmount) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        Company company = employee.getCompany();

        BigDecimal amount = scale(requestedAmount != null ? requestedAmount : employee.getMonthlySalary());
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "No salary amount is set for this employee.");
        }

        List<PersonalObligation> active = obligationRepository
                .findByAccountIdAndAccountTypeAndActiveTrueOrderByCreatedAtAsc(employeeId, PersonalAccountType.EMPLOYEE);
        BigDecimal safe = scale(active.stream()
                .map(PersonalObligation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .min(amount);
        BigDecimal spendable = scale(amount.subtract(safe)).max(BigDecimal.ZERO);

        PersonalWallet wallet = walletRepository
                .findByAccountIdAndAccountType(employeeId, PersonalAccountType.EMPLOYEE)
                .orElseGet(() -> createWallet(employeeId));
        wallet.setFreeBalance(scale(wallet.getFreeBalance().add(spendable)));
        wallet.setLockedBalance(scale(wallet.getLockedBalance().add(safe)));

        String reference = "MLR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        Instant now = Instant.now();

        PersonalSalaryEvent event = new PersonalSalaryEvent();
        event.setAccountId(employeeId);
        event.setAccountType(PersonalAccountType.EMPLOYEE);
        event.setSourceName(company.getName());
        event.setAmount(amount);
        event.setSpendableAmount(spendable);
        event.setSafeAmount(safe);
        event.setPaidAt(now);
        salaryEventRepository.save(event);

        transactionRepository.save(salaryTransaction(employeeId, company.getName(), amount, reference));
        if (safe.signum() > 0) {
            transactionRepository.save(sweepTransaction(employeeId, safe, sweepLabel(active)));
        }

        notificationService.salaryReceived(employee, company.getName(), amount, spendable, safe);
        return event;
    }

    private PersonalTransaction salaryTransaction(UUID accountId, String source, BigDecimal amount, String reference) {
        PersonalTransaction txn = baseTransaction(accountId, TransactionType.SALARY, amount);
        txn.setTitle("Salary from " + source);
        txn.setSubtitle(reference);
        txn.setDescription("Salary from " + source);
        return txn;
    }

    private PersonalTransaction sweepTransaction(UUID accountId, BigDecimal amount, String label) {
        PersonalTransaction txn = baseTransaction(accountId, TransactionType.SWEEP, amount);
        txn.setTitle("Swept to safe wallet");
        txn.setSubtitle(label);
        txn.setDescription("Swept to safe wallet");
        return txn;
    }

    private PersonalTransaction baseTransaction(UUID accountId, TransactionType type, BigDecimal amount) {
        PersonalTransaction txn = new PersonalTransaction();
        txn.setAccountId(accountId);
        txn.setAccountType(PersonalAccountType.EMPLOYEE);
        txn.setType(type);
        txn.setAmount(amount);
        txn.setTotal(amount);
        txn.setStatus(TransactionStatus.COMPLETED);
        return txn;
    }

    private String sweepLabel(List<PersonalObligation> active) {
        if (active.isEmpty()) {
            return "Bills";
        }
        return active.stream().map(PersonalObligation::getName).collect(Collectors.joining(" + "));
    }

    private PersonalWallet createWallet(UUID accountId) {
        PersonalWallet wallet = new PersonalWallet();
        wallet.setAccountId(accountId);
        wallet.setAccountType(PersonalAccountType.EMPLOYEE);
        return walletRepository.save(wallet);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
