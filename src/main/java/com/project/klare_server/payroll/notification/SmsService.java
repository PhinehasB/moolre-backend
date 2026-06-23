package com.project.klare_server.payroll.notification;

public interface SmsService {

    void send(String phone, String message);
}
