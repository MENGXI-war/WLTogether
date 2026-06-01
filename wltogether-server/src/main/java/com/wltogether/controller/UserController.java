package com.wltogether.controller;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.dto.LoginResponse;
import com.wltogether.model.dto.UpdateUserRequest;
import com.wltogether.model.entity.User;
import com.wltogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<LoginResponse.UserInfo> getMe(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return ResponseEntity.ok(toUserInfo(user));
    }

    @PutMapping("/me")
    public ResponseEntity<LoginResponse.UserInfo> updateMe(Authentication auth,
                                                            @RequestBody UpdateUserRequest request) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        user = userRepository.save(user);

        return ResponseEntity.ok(toUserInfo(user));
    }

    @GetMapping("/{id}/public-key")
    public ResponseEntity<ApiResponse<String>> getPublicKey(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return ResponseEntity.ok(ApiResponse.ok("ok", user.getPublicKey()));
    }

    private LoginResponse.UserInfo toUserInfo(User user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .publicKey(user.getPublicKey())
                .build();
    }
}
