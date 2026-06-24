package com.project.klare_server.auth.notification;

import com.project.klare_server.common.config.properties.MailProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HtmlEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(HtmlEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;
    private final String mailHost;

    public HtmlEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            MailProperties mailProperties,
            @Value("${spring.mail.host:}") String mailHost) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.mailHost = mailHost;
    }

    @Override
    public void sendCompanyVerification(String toEmail, String firstName, String companyName, String verificationLink) {
        send(toEmail, "Verify your Klare company",
                EmailTemplates.verification(firstName, companyName, verificationLink),
                "verification link: " + verificationLink);
    }

    @Override
    public void sendPasswordReset(String toEmail, String firstName, String resetLink) {
        send(toEmail, "Reset your Klare password",
                EmailTemplates.passwordReset(firstName, resetLink),
                "reset link: " + resetLink);
    }

    @Override
    public void sendEmployeeInvitation(String toEmail, String firstName, String companyName) {
        send(toEmail, "You've been added to payroll on Klare",
                EmailTemplates.employeeInvitation(firstName, companyName),
                "company: " + companyName);
    }

    @Override
    public void sendEmployeeCredentials(String toEmail, String firstName, String companyName, String username, String temporaryPassword) {
        send(toEmail, "Your Klare sign-in details",
                EmailTemplates.employeeCredentials(firstName, companyName, username, temporaryPassword),
                "username: " + username + " temp password: " + temporaryPassword);
    }

    @Override
    public void sendPayrollEstimate(String toEmail, String firstName, String companyName, String payDate, String total, int employees) {
        send(toEmail, "Your upcoming Klare payroll",
                EmailTemplates.payrollEstimate(firstName, companyName, payDate, total, employees),
                "estimate: GHS " + total + " for " + employees + " on " + payDate);
    }

    @Override
    public void sendAutomaticPayrollComplete(String toEmail, String firstName, String companyName, String amount, int paid, int failed) {
        send(toEmail, "Klare ran your payroll",
                EmailTemplates.automaticPayrollComplete(firstName, companyName, amount, paid, failed),
                "paid " + paid + " (failed " + failed + ") total GHS " + amount);
    }

    @Override
    public void sendTopUpReminder(String toEmail, String firstName, String companyName, String shortfall, String payDate) {
        send(toEmail, "Top up to run your Klare payroll",
                EmailTemplates.topUpReminder(firstName, companyName, shortfall, payDate),
                "top up GHS " + shortfall + " for payroll on " + payDate);
    }

    @Override
    public void sendPaydayReminder(String toEmail, String firstName, String companyName, String payDate) {
        send(toEmail, "Payday is almost here",
                EmailTemplates.paydayReminder(firstName, companyName, payDate),
                "payday " + payDate);
    }

    @Override
    public void sendPayrollCode(String toEmail, String firstName, String code) {
        send(toEmail, "Your Klare payroll code",
                EmailTemplates.payrollCode(firstName, code),
                "payroll code: " + code);
    }

    private void send(String to, String subject, String html, String fallbackLogLine) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || !StringUtils.hasText(mailHost)) {
            log.warn("Email not sent (SMTP not configured) -> to={} subject=\"{}\" | {}", to, subject, fallbackLogLine);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_NO, "UTF-8");
            helper.setFrom(mailProperties.fromAddress(), mailProperties.fromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("Email sent -> to={} subject=\"{}\"", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to={} subject=\"{}\" error={}", to, subject, ex.toString());
            log.warn("Email fallback -> to={} subject=\"{}\" | {}", to, subject, fallbackLogLine);
        }
    }
}
