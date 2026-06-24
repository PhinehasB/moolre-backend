package com.project.klare_server.personal.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "personal_transactions",
        indexes = @Index(name = "ix_personal_transactions_account", columnList = "account_id, account_type"))
public class PersonalTransaction extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private PersonalAccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "klare_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal klareFee = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "network", length = 40)
    private String network;

    @Column(name = "recipient", length = 60)
    private String recipient;

    @Column(name = "description", length = 160)
    private String description;

    @Column(name = "title", length = 160)
    private String title;

    @Column(name = "subtitle", length = 200)
    private String subtitle;

    @Column(name = "recipient_name", length = 120)
    private String recipientName;

    @Column(name = "external_ref", length = 80)
    private String externalRef;

    @Column(name = "transaction_id", length = 120)
    private String transactionId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.COMPLETED;
}
