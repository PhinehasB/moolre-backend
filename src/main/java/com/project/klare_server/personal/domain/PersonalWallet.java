package com.project.klare_server.personal.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "personal_wallets",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_personal_wallets_account",
                columnNames = {"account_id", "account_type"}))
public class PersonalWallet extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private PersonalAccountType accountType;

    @Column(name = "free_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal freeBalance = BigDecimal.ZERO;

    @Column(name = "locked_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GHS";
}
