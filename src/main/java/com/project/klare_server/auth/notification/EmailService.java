package com.project.klare_server.auth.notification;

public interface EmailService {

    void sendPasswordReset(String toEmail, String firstName, String resetLink);

    void sendEmployeeInvitation(String toEmail, String firstName, String companyName);
}
