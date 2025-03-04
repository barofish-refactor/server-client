package com.matsinger.barofishserver.utils.fcm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmService {
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmAdapter fcmAdapter;

    public void sendFcmByToken(FcmRequestDto requestDto) {
        List<FcmToken> tokens = fcmTokenRepository.findAllByUserId(requestDto.getTargetUserId());
        fcmAdapter.sendMessage(tokens, requestDto);
    }
}
