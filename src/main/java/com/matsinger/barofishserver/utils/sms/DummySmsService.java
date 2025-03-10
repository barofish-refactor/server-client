package com.matsinger.barofishserver.utils.sms;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "default"})
public class DummySmsService implements SmsService {
    @Override
    public void sendSms(String receiveTel, String content, String title) {
        String message = new StringBuilder()
                .append("📢 [Dummy SMS] ")
                .append(title)
                .append(": ")
                .append(content)
                .append(" (to: ")
                .append(receiveTel)
                .append(")")
                .toString();

        System.out.println(message);
    }
}