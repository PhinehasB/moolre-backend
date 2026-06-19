package com.project.klare_server.employee.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import com.project.klare_server.company.domain.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "employees",
        uniqueConstraints = @UniqueConstraint(name = "uq_employees_company_email", columnNames = {"company_id", "email"}),
        indexes = {
                @Index(name = "ix_employees_company_id", columnList = "company_id"),
                @Index(name = "ix_employees_company_status", columnList = "company_id, status")
        })
public class Employee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "monthly_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlySalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_status", nullable = false, length = 20)
    private WalletLinkStatus walletStatus = WalletLinkStatus.PROVISIONING;
}
