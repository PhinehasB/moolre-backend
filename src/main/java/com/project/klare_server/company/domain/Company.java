package com.project.klare_server.company.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "companies",
        indexes = @Index(name = "ux_companies_registration_number", columnList = "registration_number", unique = true))
public class Company extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "industry", nullable = false, length = 40)
    private Industry industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_monthly_payroll", nullable = false, length = 40)
    private PayrollBand expectedMonthlyPayroll;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CompanyStatus status = CompanyStatus.PENDING_VERIFICATION;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    @Column(name = "funds_authorization_accepted_at", nullable = false)
    private Instant fundsAuthorizationAcceptedAt;
}
