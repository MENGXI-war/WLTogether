package com.wltogether.service;

import com.wltogether.model.dto.*;
import com.wltogether.model.entity.User;
import com.wltogether.repository.UserRepository;
import com.wltogether.security.JwtTokenProvider;
import com.wltogether.security.RefreshTokenBlacklist;
import com.wltogether.security.VerificationCodeStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenBlacklist refreshTokenBlacklist;
    private final VerificationCodeStore verificationCodeStore;
    private final MailService mailService;

    private static final int MAX_DAILY_EMAILS = 20;

    @Transactional
    public ApiResponse<Void> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("EMAIL_TAKEN", "邮箱已被注册");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("USERNAME_TAKEN", "用户名已被占用");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getUsername())
                .role("USER")
                .status("PENDING")
                .emailVerified(false)
                .uid(generateUid())
                .build();
        userRepository.save(user);

        sendCode(request.getEmail());
        return ApiResponse.ok("验证邮件已发送至 " + request.getEmail());
    }

    public ApiResponse<Void> sendCode(String email) {
        if (verificationCodeStore.dailyCount(email) >= MAX_DAILY_EMAILS) {
            throw new AuthException("RATE_LIMITED", "今日发送次数已达上限");
        }
        if (!verificationCodeStore.canResend(email)) {
            long seconds = verificationCodeStore.secondsUntilResendAllowed(email);
            throw new AuthException("RATE_LIMITED", "请 " + seconds + " 秒后重试");
        }

        String code = verificationCodeStore.generate(email);
        mailService.sendVerificationCode(email, code);
        log.info("Verification code for {}: {}", email, code);
        return ApiResponse.ok("验证邮件已重新发送");
    }

    @Transactional
    public LoginResponse verifyEmail(VerifyCodeRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "用户不存在"));

        if ("ACTIVE".equals(user.getStatus())) {
            throw new AuthException("ALREADY_VERIFIED", "邮箱已验证，请直接登录");
        }

        if (verificationCodeStore.isExpired(request.getEmail())) {
            throw new AuthException("CODE_EXPIRED", "验证码已过期，请重新发送");
        }
        if (verificationCodeStore.isExhausted(request.getEmail())) {
            throw new AuthException("CODE_EXHAUSTED", "验证码已作废（错误次数过多），请重新发送");
        }

        if (!verificationCodeStore.verify(request.getEmail(), request.getCode())) {
            int remaining = verificationCodeStore.remainingAttempts(request.getEmail());
            if (remaining <= 0) {
                throw new AuthException("CODE_EXHAUSTED", "验证码已作废（错误次数过多），请重新发送");
            }
            throw new AuthException("CODE_INCORRECT", "验证码错误，剩余 " + remaining + " 次尝试");
        }

        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        userRepository.save(user);

        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailOrUsername(request.getAccount(), request.getAccount())
                .orElseThrow(() -> new AuthException("BAD_CREDENTIALS", "账号或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("BAD_CREDENTIALS", "账号或密码错误");
        }

        if ("PENDING".equals(user.getStatus())) {
            throw new AuthException("EMAIL_NOT_VERIFIED", "邮箱未验证，请查收验证邮件");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new AuthException("ACCOUNT_DISABLED", "账号已被禁用");
        }

        return buildLoginResponse(user);
    }

    public LoginResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtTokenProvider.validateRefreshToken(request.getRefreshToken());
        } catch (Exception e) {
            throw new AuthException("AUTH_INVALID", "Refresh Token 无效或已过期");
        }

        String tokenId = claims.getId();
        if (tokenId != null && refreshTokenBlacklist.isBlacklisted(tokenId)) {
            throw new AuthException("AUTH_INVALID", "Refresh Token 已被吊销");
        }

        Long userId = jwtTokenProvider.getUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "用户不存在"));

        if (tokenId != null) {
            refreshTokenBlacklist.blacklist(tokenId, 60 * 60 * 24 * 30);
        }

        return buildLoginResponse(user);
    }

    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "该邮箱未注册"));

        if (verificationCodeStore.dailyCount(request.getEmail()) >= MAX_DAILY_EMAILS) {
            throw new AuthException("RATE_LIMITED", "今日发送次数已达上限");
        }
        if (!verificationCodeStore.canResend(request.getEmail())) {
            long seconds = verificationCodeStore.secondsUntilResendAllowed(request.getEmail());
            throw new AuthException("RATE_LIMITED", "请 " + seconds + " 秒后重试");
        }

        String code = verificationCodeStore.generate(request.getEmail());
        mailService.sendResetCode(request.getEmail(), code);
        return ApiResponse.ok("重置密码验证码已发送至 " + request.getEmail());
    }

    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "用户不存在"));

        if (verificationCodeStore.isExpired(request.getEmail())) {
            throw new AuthException("CODE_EXPIRED", "验证码已过期，请重新发送");
        }

        if (!verificationCodeStore.verify(request.getEmail(), request.getCode())) {
            int remaining = verificationCodeStore.remainingAttempts(request.getEmail());
            throw new AuthException("CODE_INCORRECT", "验证码错误，剩余 " + remaining + " 次尝试");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.ok("密码重置成功，请重新登录");
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(7200)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .uid(user.getUid())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole())
                        .publicKey(user.getPublicKey())
                        .build())
                .build();
    }

    private String generateUid() {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 10; i++) {
            int num = 10000000 + random.nextInt(90000000);
            String uid = String.valueOf(num);
            if (!userRepository.existsByUid(uid)) {
                return uid;
            }
        }
        throw new RuntimeException("Failed to generate unique UID after 10 attempts");
    }
}
