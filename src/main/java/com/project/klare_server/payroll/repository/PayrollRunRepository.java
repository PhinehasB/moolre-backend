package com.project.klare_server.payroll.repository;

import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    boolean existsByCompanyIdAndPeriodYearAndPeriodMonthAndLiveModeAndStatusIn(
            UUID companyId, int periodYear, int periodMonth, boolean liveMode, Collection<PayrollRunStatus> statuses);

    Optional<PayrollRun> findByIdAndCompanyId(UUID id, UUID companyId);

    List<PayrollRun> findByCompanyIdAndStatus(UUID companyId, PayrollRunStatus status);

    List<PayrollRun> findTop12ByCompanyIdAndStatusAndLiveModeOrderByCompletedAtDesc(
            UUID companyId, PayrollRunStatus status, boolean liveMode);

    List<PayrollRun> findTop100ByCompanyIdAndLiveModeOrderByCreatedAtDesc(UUID companyId, boolean liveMode);

    List<PayrollRun> findByCompanyIdAndStatusAndPeriodYearAndLiveModeOrderByPeriodMonthDesc(
            UUID companyId, PayrollRunStatus status, int periodYear, boolean liveMode);
}
