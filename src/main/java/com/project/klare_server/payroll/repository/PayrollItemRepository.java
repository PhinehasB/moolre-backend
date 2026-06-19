package com.project.klare_server.payroll.repository;

import com.project.klare_server.payroll.domain.PayrollItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {

    List<PayrollItem> findByPayrollRunIdOrderByEmployeeNameAsc(UUID payrollRunId);
}
