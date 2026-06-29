package com.project.klare_server.personal.service;

import com.project.klare_server.personal.domain.PersonalDevice;
import com.project.klare_server.personal.repository.PersonalDeviceRepository;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalDeviceService {

    private final PersonalDeviceRepository deviceRepository;

    public PersonalDeviceService(PersonalDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public void register(AuthenticatedPersonalUser principal, String expoPushToken, String platform) {
        PersonalDevice device = deviceRepository.findByExpoPushToken(expoPushToken).orElseGet(PersonalDevice::new);
        device.setAccountId(principal.id());
        device.setAccountType(principal.accountType());
        device.setExpoPushToken(expoPushToken);
        device.setPlatform(platform);
        device.setLastSeenAt(Instant.now());
        deviceRepository.save(device);
    }

    @Transactional
    public void unregister(String expoPushToken) {
        deviceRepository.deleteByExpoPushToken(expoPushToken);
    }
}
