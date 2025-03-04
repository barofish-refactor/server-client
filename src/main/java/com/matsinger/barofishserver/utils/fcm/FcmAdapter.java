package com.matsinger.barofishserver.utils.fcm;

import java.util.List;

public interface FcmAdapter {

    void sendMessage(List<FcmToken> tokens, FcmRequestDto requestDto);
}