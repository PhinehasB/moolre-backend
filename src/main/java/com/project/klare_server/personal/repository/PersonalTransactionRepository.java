package com.project.klare_server.personal.repository;

import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalTransactionRepository extends JpaRepository<PersonalTransaction, UUID> {

    List<PersonalTransaction> findTop50ByAccountIdAndAccountTypeOrderByCreatedAtDesc(
            UUID accountId, PersonalAccountType accountType);
}
