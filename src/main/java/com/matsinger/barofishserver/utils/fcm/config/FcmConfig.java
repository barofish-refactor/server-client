package com.matsinger.barofishserver.utils.fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
@Profile("!local")
public class FcmConfig {
    @Value("${fcm.key.path}")
    private String keyFilePath;

    @Bean
    @PostConstruct
    public FirebaseMessaging firebaseMessaging() throws IOException {

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
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
