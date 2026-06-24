package com.project.klare_server.personal.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
        name = "personal_salary_events",
        indexes = @Index(name = "ix_personal_salary_events_account", columnList = "account_id, account_type"))
public class PersonalSalaryEvent extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private PersonalAccountType accountType;

    @Column(name = "source_name", nullable = false, length = 160)
    private String sourceName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "spendable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal spendableAmount;

    @Column(name = "safe_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal safeAmount;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
}
