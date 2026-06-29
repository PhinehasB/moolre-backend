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
        name = "personal_obligations",
        indexes = @Index(name = "ix_personal_obligations_account", columnList = "account_id, account_type"))
public class PersonalObligation extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private PersonalAccountType accountType;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ObligationCategory category = ObligationCategory.OTHER;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private ObligationFrequency frequency = ObligationFrequency.MONTHLY;

    @Column(name = "auto_swept", nullable = false, columnDefinition = "boolean default true")
    private boolean autoSwept = true;

    @Column(name = "locked", nullable = false, columnDefinition = "boolean default true")
    private boolean locked = true;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(name = "destination", length = 120)
    private String destination;

    @Column(name = "network", length = 40)
    private String network;

    @Column(name = "recipient_number", length = 60)
    private String recipientNumber;
}
