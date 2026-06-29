package com.project.klare_server.personal.domain;

import com.project.klare_server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "personal_devices",
        uniqueConstraints = @UniqueConstraint(name = "uq_personal_devices_token", columnNames = {"expo_push_token"}),
        indexes = @Index(name = "ix_personal_devices_account", columnList = "account_id, account_type"))
public class PersonalDevice extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private PersonalAccountType accountType;

    @Column(name = "expo_push_token", nullable = false, length = 255)
    private String expoPushToken;

    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
