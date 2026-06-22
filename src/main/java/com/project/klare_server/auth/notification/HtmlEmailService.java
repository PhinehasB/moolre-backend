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
