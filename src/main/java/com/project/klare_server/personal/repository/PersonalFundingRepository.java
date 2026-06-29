package com.project.klare_server.personal.repository;

import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalFunding;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalFundingRepository extends JpaRepository<PersonalFunding, UUID> {

    Optional<PersonalFunding> findByExternalRefAndAccountIdAndAccountType(
            String externalRef, UUID accountId, PersonalAccountType accountType);

    Optional<PersonalFunding> findByExternalRef(String externalRef);
}
