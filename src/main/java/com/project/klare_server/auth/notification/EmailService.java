package com.project.klare_server.auth.notification;

public interface EmailService {

    void sendCompanyVerification(String toEmail, String firstName, String companyName, String verificationLink);

    void sendPasswordReset(String toEmail, String firstName, String resetLink);

    void sendEmployeeInvitation(String toEmail, String firstName, String companyName);

    void sendEmployeeCredentials(String toEmail, String firstName, String companyName, String username, String temporaryPassword);

    void sendPayrollEstimate(String toEmail, String firstName, String companyName, String payDate, String total, int employees);

    void sendAutomaticPayrollComplete(String toEmail, String firstName, String companyName, String amount, int paid, int failed);

    void sendTopUpReminder(String toEmail, String firstName, String companyName, String shortfall, String payDate);

    void sendPaydayReminder(String toEmail, String firstName, String companyName, String payDate);

    void sendPayrollCode(String toEmail, String firstName, String code);
}
