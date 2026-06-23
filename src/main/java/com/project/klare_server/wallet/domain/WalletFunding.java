package com.project.klare_server.wallet.domain;

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
        name = "wallet_fundings",
        uniqueConstraints = @UniqueConstraint(name = "uq_wallet_fundings_external_ref", columnNames = "external_ref"),
        indexes = @Index(name = "ix_wallet_fundings_company_id", columnList = "company_id"))
public class WalletFunding extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "external_ref", nullable = false, length = 64)
    private String externalRef;

    @Column(name = "payer", nullable = false, length = 20)
    private String payer;

    @Column(name = "channel", nullable = false, length = 5)
    private String channel;

    @Column(name = "source", nullable = false, length = 10)
    private String source = "MOMO";

    @Column(name = "live_mode", nullable = false)
    private boolean liveMode = true;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FundingStatus status;

    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "credited", nullable = false)
    private boolean credited = false;

    @Column(name = "message", length = 255)
    private String message;
}
