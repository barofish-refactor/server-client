package com.matsinger.barofishserver.domain.notification.application;

import com.matsinger.barofishserver.domain.notification.domain.Notification;
import com.matsinger.barofishserver.domain.notification.repository.NotificationRepository;
import com.matsinger.barofishserver.utils.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationQueryService {
    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;
    public Page<Notification> selectNotificationListWithUserId(Integer userId, PageRequest pageRequest) {
        return notificationRepository.findAllByUserId(userId, pageRequest);
    }

    public Integer countAllNotificationByUserId(Integer userId) {
        return notificationRepository.countAllByUserId(userId);
    }
}
