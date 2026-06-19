package com.project.klare_server.payroll.domain;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "payroll_runs",
        indexes = @Index(name = "ix_payroll_runs_company_id", columnList = "company_id"))
public class PayrollRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayrollRunStatus status = PayrollRunStatus.PENDING_CONFIRMATION;

    @Column(name = "employee_count", nullable = false)
    private int employeeCount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "success_count", nullable = false)
    private int successCount = 0;

    @Column(name = "failure_count", nullable = false)
    private int failureCount = 0;

    @Column(name = "initiated_by_user_id", nullable = false)
    private UUID initiatedByUserId;

    @Column(name = "confirmation_code_hash", length = 64)
    private String confirmationCodeHash;

    @Column(name = "confirmation_phone_last4", length = 4)
    private String confirmationPhoneLast4;

    @Column(name = "confirmation_expires_at")
    private Instant confirmationExpiresAt;

    @Column(name = "confirmation_attempts", nullable = false)
    private int confirmationAttempts = 0;

    @Column(name = "completed_at")
    private Instant completedAt;
}
