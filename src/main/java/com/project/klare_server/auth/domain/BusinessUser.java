package com.project.klare_server.auth.domain;

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
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "business_users",
        indexes = {
                @Index(name = "ux_business_users_email", columnList = "email", unique = true),
                @Index(name = "ix_business_users_company_id", columnList = "company_id")
        })
public class BusinessUser extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private BusinessUserRole role = BusinessUserRole.OWNER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BusinessUserStatus status = BusinessUserStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;
}
