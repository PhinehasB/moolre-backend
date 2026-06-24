package com.project.klare_server.personal.service;

import com.project.klare_server.common.error.ResourceNotFoundException;
import com.project.klare_server.personal.domain.PersonalSalaryEvent;
import com.project.klare_server.personal.dto.PendingSalaryResponse;
import com.project.klare_server.personal.repository.PersonalSalaryEventRepository;
import com.project.klare_server.personal.repository.PersonalWalletRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalSalaryService {

    private final PersonalSalaryEventRepository salaryEventRepository;
    private final PersonalWalletRepository walletRepository;

    public PersonalSalaryService(
            PersonalSalaryEventRepository salaryEventRepository, PersonalWalletRepository walletRepository) {
        this.salaryEventRepository = salaryEventRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(readOnly = true)
    public PendingSalaryResponse pending(AuthenticatedPersonalUser principal) {
        return salaryEventRepository
                .findFirstByAccountIdAndAccountTypeAndAcknowledgedAtIsNullOrderByPaidAtDesc(
                        principal.id(), principal.accountType())
                .map(event -> PendingSalaryResponse.from(event, currency(principal)))
                .orElse(null);
    }

    @Transactional
    public void acknowledge(AuthenticatedPersonalUser principal, UUID eventId) {
        PersonalSalaryEvent event = salaryEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary update not found"));
        if (!event.getAccountId().equals(principal.id()) || event.getAccountType() != principal.accountType()) {
            throw new ResourceNotFoundException("Salary update not found");
        }
        if (event.getAcknowledgedAt() == null) {
            event.setAcknowledgedAt(Instant.now());
        }
    }

    private String currency(AuthenticatedPersonalUser principal) {
        return walletRepository.findByAccountIdAndAccountType(principal.id(), principal.accountType())
                .map(wallet -> wallet.getCurrency())
                .orElse("GHS");
    }
}
