package com.project.klare_server.personal.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import com.project.klare_server.wallet.domain.FundingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "personal_fundings",
        uniqueConstraints = @UniqueConstraint(name = "uq_personal_fundings_ref", columnNames = {"external_ref"}),
        indexes = @Index(name = "ix_personal_fundings_account", columnList = "account_id, account_type"))
public class PersonalFunding extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private PersonalAccountType accountType;

    @Column(name = "external_ref", nullable = false, length = 80)
    private String externalRef;

    @Column(name = "payer", length = 30)
    private String payer;

    @Column(name = "channel", length = 10)
    private String channel;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FundingStatus status = FundingStatus.AWAITING_APPROVAL;

    @Column(name = "transaction_id", length = 120)
    private String transactionId;

    @Column(name = "credited", nullable = false, columnDefinition = "boolean default false")
    private boolean credited = false;

    @Column(name = "message", length = 255)
    private String message;
}
