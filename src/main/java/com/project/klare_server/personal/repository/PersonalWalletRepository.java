package com.project.klare_server.personal.repository;

import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalWallet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalWalletRepository extends JpaRepository<PersonalWallet, UUID> {

    Optional<PersonalWallet> findByAccountIdAndAccountType(UUID accountId, PersonalAccountType accountType);
}
