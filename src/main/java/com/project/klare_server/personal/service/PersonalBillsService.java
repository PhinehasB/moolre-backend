package com.project.klare_server.personal.service;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalObligation;
import com.project.klare_server.personal.dto.CreateObligationRequest;
import com.project.klare_server.personal.dto.PersonalBillsResponse;
import com.project.klare_server.personal.repository.PersonalObligationRepository;
import com.project.klare_server.personal.repository.PersonalWalletRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PersonalBillsService {

    private final EmployeeRepository employeeRepository;
    private final PersonalObligationRepository obligationRepository;
    private final PersonalWalletRepository walletRepository;

    public PersonalBillsService(
            EmployeeRepository employeeRepository,
            PersonalObligationRepository obligationRepository,
            PersonalWalletRepository walletRepository) {
        this.employeeRepository = employeeRepository;
        this.obligationRepository = obligationRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public PersonalBillsResponse.Bill create(AuthenticatedPersonalUser principal, CreateObligationRequest request) {
        PersonalObligation obligation = new PersonalObligation();
        obligation.setAccountId(principal.id());
        obligation.setAccountType(principal.accountType());
        obligation.setName(request.name().trim());
        obligation.setCategory(request.category());
        obligation.setAmount(request.amount());
        obligation.setActive(true);
        obligation.setLocked(true);
        obligation.setAutoSwept(true);
        obligation.setDestination(buildDestination(request.network(), request.recipientNumber()));
        return toBill(obligationRepository.save(obligation));
    }

    private String buildDestination(String network, String recipientNumber) {
        String label = network.trim();
        String number = StringUtils.hasText(recipientNumber) ? recipientNumber.trim() : "";
        if ("BANK".equalsIgnoreCase(label)) {
            return number.isEmpty() ? "To bank" : "To bank " + number;
        }
        return number.isEmpty() ? "To " + label : "To " + label + " " + number;
    }

    @Transactional(readOnly = true)
    public PersonalBillsResponse bills(AuthenticatedPersonalUser principal) {
        List<PersonalObligation> obligations = obligationRepository
                .findByAccountIdAndAccountTypeOrderByCreatedAtAsc(principal.id(), principal.accountType());

        BigDecimal locked = obligations.stream()
                .filter(PersonalObligation::isActive)
                .map(PersonalObligation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal income = monthlyIncome(principal);
        BigDecimal spendable = income == null ? null : income.subtract(locked).max(BigDecimal.ZERO);
        Integer sweepPercentage = (income == null || income.signum() <= 0)
                ? null
                : locked.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP).intValue();

        List<PersonalBillsResponse.Bill> bills = obligations.stream().map(this::toBill).toList();
        return new PersonalBillsResponse(income, locked, spendable, sweepPercentage, currency(principal), bills);
    }

    private BigDecimal monthlyIncome(AuthenticatedPersonalUser principal) {
        if (principal.accountType() != PersonalAccountType.EMPLOYEE) {
            return null;
        }
        Employee employee = employeeRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return employee.getMonthlySalary();
    }

    private String currency(AuthenticatedPersonalUser principal) {
        return walletRepository.findByAccountIdAndAccountType(principal.id(), principal.accountType())
                .map(wallet -> wallet.getCurrency())
                .orElse("GHS");
    }

    private PersonalBillsResponse.Bill toBill(PersonalObligation obligation) {
        return new PersonalBillsResponse.Bill(
                obligation.getId(),
                obligation.getName(),
                obligation.getCategory().name(),
                obligation.getAmount(),
                obligation.getDestination(),
                obligation.isActive(),
                obligation.isLocked());
    }
}
