package com.project.klare_server.payroll.notification;

public interface SmsService {

    void sendPayrollConfirmationCode(String phone, String code);
}
