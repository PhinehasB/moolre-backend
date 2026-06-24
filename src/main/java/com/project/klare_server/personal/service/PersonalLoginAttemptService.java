package com.project.klare_server.personal.service;

import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.employee.repository.EmployeeRepository;
import com.project.klare_server.personal.repository.PersonalUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalLoginAttemptService {

    private final EmployeeRepository employeeRepository;
    private final PersonalUserRepository personalUserRepository;
    private final SecurityProperties properties;

    public PersonalLoginAttemptService(
            EmployeeRepository employeeRepository,
            PersonalUserRepository personalUserRepository,
            SecurityProperties properties) {
        this.employeeRepository = employeeRepository;
        this.personalUserRepository = personalUserRepository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEmployeeFailure(UUID employeeId) {
        employeeRepository.findById(employeeId).ifPresent(employee -> {
            int attempts = employee.getFailedLoginAttempts() + 1;
            if (attempts >= properties.login().maxFailedAttempts()) {
                employee.setFailedLoginAttempts(0);
                employee.setLockedUntil(Instant.now().plus(properties.login().lockDuration()));
            } else {
                employee.setFailedLoginAttempts(attempts);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndividualFailure(UUID personalUserId) {
        personalUserRepository.findById(personalUserId).ifPresent(user -> {
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
