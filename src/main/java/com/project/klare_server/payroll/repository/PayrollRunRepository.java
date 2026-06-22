package com.project.klare_server.payroll.repository;

import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Optional<PayrollRun> findByIdAndCompanyId(UUID id, UUID companyId);

    List<PayrollRun> findByCompanyIdAndStatus(UUID companyId, PayrollRunStatus status);

    List<PayrollRun> findTop12ByCompanyIdAndStatusOrderByCompletedAtDesc(UUID companyId, PayrollRunStatus status);

    List<PayrollRun> findTop20ByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
