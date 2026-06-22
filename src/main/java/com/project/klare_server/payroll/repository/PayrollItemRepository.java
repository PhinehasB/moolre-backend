package com.project.klare_server.payroll.repository;

import com.project.klare_server.payroll.domain.PayrollItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {

    List<PayrollItem> findByPayrollRunIdOrderByEmployeeNameAsc(UUID payrollRunId);

    @Query("""
            select count(distinct i.employee.id) from PayrollItem i
            where i.payrollRun.company.id = :companyId
              and i.payrollRun.periodYear = :year
              and i.status = com.project.klare_server.payroll.domain.PayrollItemStatus.PAID
            """)
    long countDistinctPaidEmployees(@Param("companyId") UUID companyId, @Param("year") int year);
}
