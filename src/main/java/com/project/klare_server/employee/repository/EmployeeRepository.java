package com.project.klare_server.employee.repository;

import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.employee.domain.EmployeeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    boolean existsByCompanyIdAndEmailIgnoreCase(UUID companyId, String email);

    long countByCompanyId(UUID companyId);

    long countByCompanyIdAndStatus(UUID companyId, EmployeeStatus status);

    long countByCompanyIdAndCreatedAtGreaterThanEqual(UUID companyId, Instant from);

    List<Employee> findTop5ByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    @Query("""
            select coalesce(sum(e.monthlySalary), 0) from Employee e
            where e.company.id = :companyId and e.status = :status
            """)
    BigDecimal sumMonthlySalaryByStatus(@Param("companyId") UUID companyId, @Param("status") EmployeeStatus status);

    @Query("""
            select e from Employee e
            where e.company.id = :companyId
              and (:status is null or e.status = :status)
              and (:term is null
                   or lower(concat(e.firstName, ' ', e.lastName)) like :term
                   or lower(e.email) like :term
                   or e.phone like :term)
            """)
    Page<Employee> search(
            @Param("companyId") UUID companyId,
            @Param("status") EmployeeStatus status,
            @Param("term") String term,
            Pageable pageable);
}
