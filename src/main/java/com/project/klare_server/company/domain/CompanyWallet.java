package com.project.klare_server.company.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "company_wallets",
        indexes = @Index(name = "ux_company_wallets_company_id", columnList = "company_id", unique = true))
public class CompanyWallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "pending", nullable = false, precision = 19, scale = 2)
    private BigDecimal pending = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GHS";

    @Column(name = "moolre_account_ref", length = 64)
    private String moolreAccountRef;
}
