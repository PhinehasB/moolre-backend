package com.project.klare_server.auth.service;

import com.project.klare_server.auth.repository.BusinessUserRepository;
import com.project.klare_server.common.config.properties.SecurityProperties;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final BusinessUserRepository businessUserRepository;
    private final SecurityProperties properties;

    public LoginAttemptService(BusinessUserRepository businessUserRepository, SecurityProperties properties) {
        this.businessUserRepository = businessUserRepository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId) {
        businessUserRepository.findById(userId).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= properties.login().maxFailedAttempts()) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(Instant.now().plus(properties.login().lockDuration()));
            } else {
                user.setFailedLoginAttempts(attempts);
            }
        });
    }
}
