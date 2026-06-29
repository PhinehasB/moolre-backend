package com.project.klare_server.personal.repository;

import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalDevice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalDeviceRepository extends JpaRepository<PersonalDevice, UUID> {

    List<PersonalDevice> findByAccountIdAndAccountType(UUID accountId, PersonalAccountType accountType);

    Optional<PersonalDevice> findByExpoPushToken(String expoPushToken);

    void deleteByExpoPushToken(String expoPushToken);
}
