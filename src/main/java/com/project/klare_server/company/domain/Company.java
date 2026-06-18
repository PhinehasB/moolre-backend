package com.project.klare_server.company.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "companies",
        indexes = {
                @Index(name = "ux_companies_email", columnList = "email", unique = true),
                @Index(name = "ux_companies_registration_number", columnList = "registration_number", unique = true)
        })
public class Company extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "country", nullable = false, length = 2)
    private String country = "GH";

    @Column(name = "moolre_wallet_number", length = 50)
    private String moolreWalletNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CompanyStatus status = CompanyStatus.PENDING_VERIFICATION;
}
