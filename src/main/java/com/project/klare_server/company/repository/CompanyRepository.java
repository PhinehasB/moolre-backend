package com.project.klare_server.company.repository;

import com.project.klare_server.company.domain.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Company> findByEmailIgnoreCase(String email);
}
