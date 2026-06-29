package com.project.klare_server.personal.notification;

import com.project.klare_server.auth.notification.EmailService;
import com.project.klare_server.employee.domain.Employee;
import com.project.klare_server.payroll.notification.SmsService;
import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalDevice;
import com.project.klare_server.personal.repository.PersonalDeviceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PersonalNotificationService {

    private final EmailService emailService;
    private final SmsService smsService;
    private final ExpoPushService expoPushService;
    private final PersonalDeviceRepository deviceRepository;

    public PersonalNotificationService(
            EmailService emailService,
            SmsService smsService,
            ExpoPushService expoPushService,
            PersonalDeviceRepository deviceRepository) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.expoPushService = expoPushService;
        this.deviceRepository = deviceRepository;
    }

    public void salaryReceived(Employee employee, String companyName, BigDecimal amount, BigDecimal spendable, BigDecimal safe) {
        String first = employee.getFirstName();
        String amountText = amount.toPlainString();
        String spendableText = spendable.toPlainString();
        String safeText = safe.toPlainString();

        if (StringUtils.hasText(employee.getEmail())) {
            emailService.sendSalaryReceived(employee.getEmail(), first, companyName, amountText, spendableText, safeText);
        }
        if (StringUtils.hasText(employee.getPhone())) {
            smsService.send(employee.getPhone(), "Hi " + first + ", GHS " + amountText + " from " + companyName
                    + " just landed on Klare. GHS " + spendableText + " is yours to spend, GHS " + safeText
                    + " locked for bills. Open Klare to review.");
        }

        List<String> tokens = deviceRepository
                .findByAccountIdAndAccountType(employee.getId(), PersonalAccountType.EMPLOYEE)
                .stream()
                .map(PersonalDevice::getExpoPushToken)
                .toList();
        expoPushService.send(
                tokens,
                "Salary received 🎉",
                "GHS " + amountText + " from " + companyName + " landed. GHS " + spendableText
                        + " is yours to spend — tap to review your bills.",
                Map.of("type", "SALARY"));
    }
}
