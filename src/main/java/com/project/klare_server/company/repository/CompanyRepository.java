package com.project.klare_server.company.repository;

import com.project.klare_server.company.domain.Company;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByRegistrationNumber(String registrationNumber);

    List<Company> findByAutoPayrollEnabledTrue();
}
