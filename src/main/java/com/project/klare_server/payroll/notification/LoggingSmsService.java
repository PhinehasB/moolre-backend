package com.project.klare_server.payroll.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsService.class);

    @Override
    public void sendPayrollConfirmationCode(String phone, String code) {
        log.info("Payroll confirmation SMS -> to={} code={}", phone, code);
    }
}
