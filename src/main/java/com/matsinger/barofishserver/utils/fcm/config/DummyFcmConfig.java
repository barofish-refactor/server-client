package com.matsinger.barofishserver.utils.fcm.config;

import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "default"})
public class DummyFcmConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return null;
    }
}