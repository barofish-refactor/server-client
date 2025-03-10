package com.matsinger.barofishserver.utils.fcm;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Profile({"local", "default"})
public class DummyFcmAdapter implements FcmAdapter {

    @Override
    public void sendMessage(List<FcmToken> tokens, FcmRequestDto requestDto) {
        System.out.println("📢 [Dummy FCM] " + requestDto.getTitle() + ": " + requestDto.getBody() + " (to: " + requestDto.getTargetUserId() + ")");
    }
}