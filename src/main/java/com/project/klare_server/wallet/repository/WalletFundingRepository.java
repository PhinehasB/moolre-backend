package com.project.klare_server.wallet.repository;

import com.project.klare_server.wallet.domain.WalletFunding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletFundingRepository extends JpaRepository<WalletFunding, UUID> {

    Optional<WalletFunding> findByExternalRef(String externalRef);

    Optional<WalletFunding> findByExternalRefAndCompanyId(String externalRef, UUID companyId);

    List<WalletFunding> findTop10ByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
