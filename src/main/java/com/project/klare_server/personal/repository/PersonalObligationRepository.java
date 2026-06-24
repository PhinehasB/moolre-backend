package com.project.klare_server.personal.repository;

import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalObligation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalObligationRepository extends JpaRepository<PersonalObligation, UUID> {

    List<PersonalObligation> findByAccountIdAndAccountTypeOrderByCreatedAtAsc(
            UUID accountId, PersonalAccountType accountType);

    List<PersonalObligation> findByAccountIdAndAccountTypeAndActiveTrueOrderByCreatedAtAsc(
            UUID accountId, PersonalAccountType accountType);
}
