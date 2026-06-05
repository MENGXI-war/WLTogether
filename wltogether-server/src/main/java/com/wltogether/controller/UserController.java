package com.wltogether.controller;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.dto.ChangePasswordRequest;
import com.wltogether.model.dto.LoginResponse;
import com.wltogether.model.dto.UpdateUserRequest;
import com.wltogether.model.entity.User;
import com.wltogether.repository.UserRepository;
import com.wltogether.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

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
        if (request.getPublicKey() != null) {
            user.setPublicKey(request.getPublicKey());
        }
        user = userRepository.save(user);

        return ResponseEntity.ok(toUserInfo(user));
    }

    @GetMapping("/by-uid/{uid}")
    public ResponseEntity<LoginResponse.UserInfo> getByUid(@PathVariable String uid) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return ResponseEntity.ok(toUserInfo(user));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(Authentication auth,
                                                             @RequestParam("file") MultipartFile file) {
        Long userId = (Long) auth.getPrincipal();
        try {
            String avatarUrl = userService.uploadAvatar(userId, file);
            return ResponseEntity.ok(ApiResponse.ok("头像上传成功", avatarUrl));
        } catch (IOException e) {
            throw new IllegalArgumentException("头像处理失败: " + e.getMessage());
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication auth,
                                                             @RequestBody ChangePasswordRequest request) {
        Long userId = (Long) auth.getPrincipal();
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("密码修改成功"));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        userService.deleteAvatar(userId);
        return ResponseEntity.ok(ApiResponse.ok("头像已删除"));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        try {
            byte[] imageBytes = userService.getAvatar(id);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(404).build();
        }
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
                .uid(user.getUid())
                .email(user.getEmail())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .publicKey(user.getPublicKey())
                .build();
    }
}
