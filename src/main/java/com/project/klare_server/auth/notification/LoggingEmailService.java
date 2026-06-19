package com.project.klare_server.auth.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendPasswordReset(String toEmail, String firstName, String resetLink) {
        log.info("Password reset email -> to={} name={} link={}", toEmail, firstName, resetLink);
    }

    @Override
    public void sendEmployeeInvitation(String toEmail, String firstName, String companyName) {
        log.info("Employee invitation email -> to={} name={} company={}", toEmail, firstName, companyName);
    }
}
