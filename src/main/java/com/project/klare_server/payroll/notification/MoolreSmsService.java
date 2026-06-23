package com.project.klare_server.payroll.notification;

import com.project.klare_server.moolre.MoolreClient;
import org.springframework.stereotype.Service;

@Service
public class MoolreSmsService implements SmsService {

    private final MoolreClient moolreClient;

    public MoolreSmsService(MoolreClient moolreClient) {
        this.moolreClient = moolreClient;
    }

    @Override
    public void send(String phone, String message) {
        moolreClient.sendSms(phone, message);
    }
}
