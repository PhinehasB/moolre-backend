package com.project.klare_server.notification;

import com.project.klare_server.auth.notification.EmailService;
import com.project.klare_server.payroll.notification.SmsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationService(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public void payrollCode(String email, String phone, String firstName, String code) {
        sms(phone, "Klare: your payroll confirmation code is " + code + ". It expires in 10 minutes. Do not share it.");
        if (StringUtils.hasText(email)) {
            emailService.sendPayrollCode(email, firstName, code);
        }
    }

    public void automaticPayrollComplete(String email, String phone, String firstName, String companyName, String amount, int paid, int failed) {
        if (StringUtils.hasText(email)) {
            emailService.sendAutomaticPayrollComplete(email, firstName, companyName, amount, paid, failed);
        }
        sms(phone, "Klare paid " + paid + " employee(s) for " + companyName + " (GHS " + amount + ")."
                + (failed > 0 ? " " + failed + " failed; we'll retry." : ""));
    }

    public void topUpReminder(String email, String phone, String firstName, String companyName, String shortfall, String payDate) {
        if (StringUtils.hasText(email)) {
            emailService.sendTopUpReminder(email, firstName, companyName, shortfall, payDate);
        }
        sms(phone, "Klare: top up GHS " + shortfall + " to run " + companyName + " payroll (due " + payDate + ").");
    }

    public void payrollEstimate(String email, String phone, String firstName, String companyName, String payDate, String total, int employees) {
        if (StringUtils.hasText(email)) {
            emailService.sendPayrollEstimate(email, firstName, companyName, payDate, total, employees);
        }
        sms(phone, "Klare runs " + companyName + " payroll on " + payDate + ": GHS " + total + " to " + employees + " staff. Keep your wallet funded.");
    }

    public void paydayReminder(String email, String phone, String firstName, String companyName, String payDate) {
        if (StringUtils.hasText(email)) {
            emailService.sendPaydayReminder(email, firstName, companyName, payDate);
        }
        sms(phone, "Hi " + firstName + ", your salary from " + companyName + " arrives on " + payDate + " via Klare.");
    }

    public void employeeInvitation(String email, String phone, String firstName, String companyName) {
        if (StringUtils.hasText(email)) {
            emailService.sendEmployeeInvitation(email, firstName, companyName);
        }
        sms(phone, "Hi " + firstName + ", " + companyName + " added you to payroll on Klare. Download the Klare app to get paid automatically.");
    }

    private void sms(String phone, String message) {
        if (StringUtils.hasText(phone)) {
            smsService.send(phone, message);
        }
    }
}
