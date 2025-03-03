package com.matsinger.barofishserver.utils.fcm;

public interface FcmService {
    void sendFcmByToken(FcmRequestDto requestDto);
}