package com.matsinger.barofishserver.utils.fcm;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("local")
public class DummyFcmService implements FcmService {

    @Override
    public void sendFcmByToken(FcmRequestDto requestDto) {
        System.out.println("📢 [Dummy FCM] " + requestDto.getTitle() + ": " + requestDto.getBody() + " (to: " + requestDto.getTargetUserId() + ")");
    }
}