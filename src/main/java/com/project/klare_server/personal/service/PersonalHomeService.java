package com.project.klare_server.personal.service;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.company.domain.Company;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalObligation;
import com.project.klare_server.personal.domain.PersonalUser;
import com.project.klare_server.personal.domain.PersonalWallet;
import com.project.klare_server.personal.dto.PersonalHomeResponse;
import com.project.klare_server.personal.repository.PersonalObligationRepository;
import com.project.klare_server.personal.repository.PersonalUserRepository;
import com.project.klare_server.personal.repository.PersonalWalletRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalHomeService {

    private static final int CYCLE_DAYS = 30;

    private final EmployeeRepository employeeRepository;
    private final PersonalUserRepository personalUserRepository;
    private final PersonalWalletRepository walletRepository;
    private final PersonalObligationRepository obligationRepository;

    public PersonalHomeService(
            EmployeeRepository employeeRepository,
            PersonalUserRepository personalUserRepository,
            PersonalWalletRepository walletRepository,
            PersonalObligationRepository obligationRepository) {
        this.employeeRepository = employeeRepository;
        this.personalUserRepository = personalUserRepository;
        this.walletRepository = walletRepository;
        this.obligationRepository = obligationRepository;
    }

    @Transactional
    public PersonalHomeResponse home(AuthenticatedPersonalUser principal) {
        PersonalWallet wallet = walletRepository
                .findByAccountIdAndAccountType(principal.id(), principal.accountType())
                .orElseGet(() -> createWallet(principal));

        PersonalHomeResponse.Wallet walletDto = new PersonalHomeResponse.Wallet(
                wallet.getCurrency(), wallet.getFreeBalance(), wallet.getLockedBalance());

        List<PersonalHomeResponse.Bill> bills = obligationRepository
                .findByAccountIdAndAccountTypeAndActiveTrueOrderByCreatedAtAsc(principal.id(), principal.accountType())
                .stream()
                .map(this::toBill)
                .toList();

        if (principal.accountType() == PersonalAccountType.EMPLOYEE) {
            Employee employee = employeeRepository.findById(principal.id())
                    .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
            return new PersonalHomeResponse(employee.getFirstName(), walletDto, nextSalary(employee), bills);
        }

        PersonalUser user = personalUserRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return new PersonalHomeResponse(user.getFirstName(), walletDto, null, bills);
    }

    private PersonalWallet createWallet(AuthenticatedPersonalUser principal) {
        PersonalWallet wallet = new PersonalWallet();
        wallet.setAccountId(principal.id());
        wallet.setAccountType(principal.accountType());
        return walletRepository.save(wallet);
    }

    private PersonalHomeResponse.NextSalary nextSalary(Employee employee) {
        Company company = employee.getCompany();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate expected = nextPayday(company.getPayrollDayOfMonth(), today);
        long daysUntil = ChronoUnit.DAYS.between(today, expected);
        double progress = Math.min(1.0, Math.max(0.05, (CYCLE_DAYS - daysUntil) / (double) CYCLE_DAYS));
        return new PersonalHomeResponse.NextSalary(
                company.getName(), employee.getMonthlySalary(), expected, daysUntil, progress);
    }

    private LocalDate nextPayday(int payrollDayOfMonth, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        LocalDate candidate = current.atDay(Math.min(payrollDayOfMonth, current.lengthOfMonth()));
        if (!candidate.isBefore(today)) {
            return candidate;
        }
        YearMonth next = current.plusMonths(1);
        return next.atDay(Math.min(payrollDayOfMonth, next.lengthOfMonth()));
    }

    private PersonalHomeResponse.Bill toBill(PersonalObligation obligation) {
        String base = obligation.getFrequency().name().charAt(0)
                + obligation.getFrequency().name().substring(1).toLowerCase();
        String frequencyLabel = obligation.isAutoSwept() ? base + " · auto-swept" : base;
        return new PersonalHomeResponse.Bill(
                obligation.getId(),
                obligation.getName(),
                obligation.getCategory().name(),
                obligation.getAmount(),
                frequencyLabel,
                obligation.isLocked(),
                obligation.isAutoSwept());
    }
}
