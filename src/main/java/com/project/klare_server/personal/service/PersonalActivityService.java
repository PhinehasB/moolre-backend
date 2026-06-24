package com.project.klare_server.personal.service;

import com.project.klare_server.personal.dto.ActivityItemResponse;
import com.project.klare_server.personal.repository.PersonalTransactionRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalActivityService {

    private final PersonalTransactionRepository transactionRepository;

    public PersonalActivityService(PersonalTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityItemResponse> list(AuthenticatedPersonalUser principal) {
        return transactionRepository
                .findTop50ByAccountIdAndAccountTypeOrderByCreatedAtDesc(principal.id(), principal.accountType())
                .stream()
                .map(ActivityItemResponse::from)
                .toList();
    }
}
