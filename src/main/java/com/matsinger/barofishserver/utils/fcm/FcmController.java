package com.matsinger.barofishserver.utils.fcm;

import com.matsinger.barofishserver.domain.notification.application.NotificationCommandService;
import com.matsinger.barofishserver.domain.notification.domain.Notification;
import com.matsinger.barofishserver.domain.notification.domain.NotificationType;
import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.domain.user.domain.UserState;
import com.matsinger.barofishserver.domain.user.repository.UserRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fcm")
public class FcmController {
    private final JwtService jwt;
    private final Common utils;
    private final FcmService fcmService;
    private final UserRepository userRepository;
    private final NotificationCommandService notificationCommandService;

    @Getter
    @NoArgsConstructor
    public static class AdminSendFcmReq {
        List<Integer> userIds;
        String title;
        String content;
    }

    @PostMapping("send-fcm")
    public ResponseEntity<CustomResponse<Boolean>> sendFcmToUser(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                 @RequestPart(value = "data") AdminSendFcmReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        String title = utils.validateString(data.getTitle(), 100L, "제목");
        if (data.content == null) throw new BusinessException("내용을 입력해주세요.");
        List<Integer> userIds = data.getUserIds();
        if (userIds == null)
            userIds = userRepository.findAllByState(UserState.ACTIVE).stream().map(User::getId).toList();
        Notification
                notification =
                Notification.builder().type(NotificationType.ADMIN).title(title).content(data.getContent()).createdAt(
                        utils.now()).build();
        for (Integer userId : userIds) {
            notification.setUserId(userId);
            notificationCommandService.addNotification(notification);
            fcmService.sendFcmByToken(FcmRequestDto.builder().targetUserId(userId).title(title).body(data.getContent()).build());
        }
        return ResponseEntity.ok(res);
    }
}
