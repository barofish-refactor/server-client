package com.matsinger.barofishserver.domain.userauth.application;

import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.domain.user.dto.SnsJoinReq;
import com.matsinger.barofishserver.domain.user.dto.UserJoinReq;
import com.matsinger.barofishserver.domain.user.dto.UserUpdateReq;
import com.matsinger.barofishserver.domain.user.exception.UserException;
import com.matsinger.barofishserver.domain.user.repository.UserRepository;
import com.matsinger.barofishserver.domain.userauth.domain.LoginType;
import com.matsinger.barofishserver.domain.userauth.domain.UserAuth;
import com.matsinger.barofishserver.domain.userauth.repository.UserAuthRepository;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.domain.userinfo.repository.UserInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.JwtProvider;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.RegexConstructor;
import com.matsinger.barofishserver.utils.sms.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserAuthCommandService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final UserInfoRepository userInfoRepository;


    private final UserException userValidator;
    private final Common util;
    private final RegexConstructor re;

    public UserAuth createUserAuth(SnsJoinReq request, User user) {
        String loginId = request.getLoginId();
        Optional<UserAuth> optionalUserAuth = userAuthRepository.findByLoginId(loginId);

        if (optionalUserAuth.isPresent()) {
            UserAuth findUserAuth = optionalUserAuth.get();
            User findUser = userRepository.findById(findUserAuth.getUserId()).get();
            userValidator.validateUserState(findUser);
            throw new BusinessException("중복된 아이디가 존재합니다.");
        }

        UserAuth userAuth = request.toUserAuth();
        userAuth.setUserId(user.getId());
//        userAuth.setUser(user);


        return userAuthRepository.save(userAuth);
    }

    public UserAuth createIdPwUserAuthAndSave(User createdUser, UserJoinReq request) {

        String email = util.validateString(request.getEmail(), 300L, "이메일");
        String password = request.getPassword();

        validate(email, password);
        Optional<UserAuth> optionalUserAuth = userAuthRepository.findByLoginTypeAndLoginId(LoginType.IDPW, email);
        if (optionalUserAuth.isPresent()) {
            throw new BusinessException("이메일 주소가 존재합니다." + "\n" + "다른 이메일 주소를 입력해주세요.");
        }

        UserAuth
                createdUserAuth =
                UserAuth.builder().userId(createdUser.getId()).loginType(LoginType.IDPW).loginId(email).password(BCrypt.hashpw(
                        password,
                        BCrypt.gensalt())).build();
        createdUserAuth.setUser(createdUser);

        return userAuthRepository.save(createdUserAuth);
    }

    public void updateUserAuth(Integer userId, UserUpdateReq request) {

        UserAuth userAuth = userAuthRepository.findFirstByUserId(userId);

        if (request.getNewPassword() == null) {
            throw new BusinessException("비밀번호를 입력해주세요.");
        }
        if (request.getOldPassword() == null) {
            throw new BusinessException("변경 전 비밀번호를 입력해주세요.");
        }
        if (!userAuth.getLoginType().equals(LoginType.IDPW)) {
            throw new BusinessException("소셜 로그인 유저입니다.");
        }
        if (!BCrypt.checkpw(request.getOldPassword(), userAuth.getPassword())) {
            throw new BusinessException("이전 비밀번호와 일치하지 않습니다.");
        }
        if (!Pattern.matches(re.password, request.getNewPassword()))
            throw new BusinessException("비밀번호 형식을 확인해주세요.");
        String password = BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt());
        userAuth.setPassword(password);
    }

    private void validate(String email, String password) {
        if (!Pattern.matches(re.email, email)) {
            throw new BusinessException("이메일 형식을 확인해주세요.");
        }

        if (!Pattern.matches(re.password, password)) {
            throw new BusinessException("비밀번호 형식을 확인해주세요.");
        }
    }

    public String resetPassword(String phoneNumber) {
        UserInfo
                findUserInfo =
                userInfoRepository.findByPhone(phoneNumber).orElseThrow(() -> new BusinessException(
                        "휴대폰 번호를 찾을 수 없습니다."));

        UserAuth findUserAuth = userAuthRepository.findFirstByUserId(findUserInfo.getUserId());

        if (findUserInfo == null) {
            new BusinessException("소셜 로그인 유저입니다.");
        }
        String newPassword = generateRandomString(6);
        findUserAuth.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userAuthRepository.save(findUserAuth);
        return newPassword;
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomType = random.nextInt(3);
            char randomChar;

            switch (randomType) {
                case 0:
                    randomChar = (char) (random.nextInt(10) + '0');  // Numbers
                    break;
                case 1:
                    randomChar = (char) (random.nextInt(26) + 'A');  // Uppercase letters
                    break;
                default:
                    randomChar = (char) (random.nextInt(26) + 'a');  // Lowercase letters
                    break;
            }

            sb.append(randomChar);
        }

        return sb.toString();
    }
}
