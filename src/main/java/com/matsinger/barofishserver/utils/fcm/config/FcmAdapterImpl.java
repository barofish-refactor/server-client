package com.matsinger.barofishserver.utils.fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import com.matsinger.barofishserver.utils.fcm.FcmAdapter;
import com.matsinger.barofishserver.utils.fcm.FcmRequestDto;
import com.matsinger.barofishserver.utils.fcm.FcmToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Profile("!local")
public class FcmAdapterImpl implements FcmAdapter {
    @Value("${fcm.key.path}")
    private String keyFilePath;
    private FirebaseMessaging firebaseMessaging;


    @Bean
    @PostConstruct
    public void firebaseMessaging() throws IOException {
        FirebaseApp firebaseApp = null;

        List<FirebaseApp> firebaseAppList = FirebaseApp.getApps();

        if (firebaseAppList != null && !firebaseAppList.isEmpty()) {
            for (FirebaseApp app : firebaseAppList) {
                if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                    firebaseApp = app;
                }
            }
        } else {
            Resource resources =
                    ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader())
                            .getResource("classpath" + ":firebase/" + keyFilePath);
            InputStream refreshToken = resources.getInputStream();

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(refreshToken))
                            .build();
            firebaseApp = FirebaseApp.initializeApp(options);
        }
        System.out.println("Fcm Setting Completed");
        firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp);
    }

    @Override
    public void sendMessage(List<FcmToken> tokens, FcmRequestDto requestDto) {
        if (tokens != null && tokens.size() != 0) {
            for (FcmToken token : tokens) {
                Notification notification = Notification.builder()
                        .setTitle(requestDto.getTitle())
                        .setBody(requestDto.getBody())
                        .build();

                AndroidNotification androidNotification = AndroidNotification.builder()
                        .setTitle(requestDto.getTitle())
                        .setBody(requestDto.getBody())
                        .setSound("default")
                        .setChannelId("barofish")
                        .build();
                AndroidConfig androidConfig = AndroidConfig.builder()
                        .setNotification(androidNotification)
                        .build();

                Aps aps = Aps.builder()
                        .setSound("default")
                        .setContentAvailable(true)
                        .build();
                ApnsConfig apnsConfig = ApnsConfig.builder()
                        .putHeader("apns-priority", "5")
                        .setAps(aps)
                        .build();

                Message message =
                        Message.builder()
                                .setToken(token.getToken())
                                .setNotification(notification)
                                .setAndroidConfig(androidConfig)
                                .setApnsConfig(apnsConfig)
                                .build();
                firebaseMessaging.sendAsync(message);
            }
        }
    }
}
