package com.project.klare_server.company.repository;

import com.project.klare_server.company.domain.CompanyWallet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyWalletRepository extends JpaRepository<CompanyWallet, UUID> {

    Optional<CompanyWallet> findByCompanyId(UUID companyId);
}
