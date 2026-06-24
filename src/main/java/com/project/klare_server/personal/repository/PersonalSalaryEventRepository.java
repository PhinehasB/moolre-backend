package com.project.klare_server.personal.repository;

import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalSalaryEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalSalaryEventRepository extends JpaRepository<PersonalSalaryEvent, UUID> {

    Optional<PersonalSalaryEvent> findFirstByAccountIdAndAccountTypeAndAcknowledgedAtIsNullOrderByPaidAtDesc(
            UUID accountId, PersonalAccountType accountType);
}
