package com.matsinger.barofishserver.utils.sms;

public interface SmsService {
    void sendSms(String receiveTel, String content, String title);
}