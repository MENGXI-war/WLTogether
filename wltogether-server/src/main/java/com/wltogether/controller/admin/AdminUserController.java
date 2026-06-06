package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.User;
import com.wltogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<User>> list(Pageable pageable,
                                            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(userRepository.findByStatus(status, pageable));
        }
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id,
                                                           @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String newStatus = body.get("status");
        if (newStatus == null || (!"ACTIVE".equals(newStatus) && !"DISABLED".equals(newStatus))) {
            throw new IllegalArgumentException("状态值无效，仅支持 ACTIVE 或 DISABLED");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("不能修改管理员状态");
        }
        user.setStatus(newStatus);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("用户状态已更新"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("不能删除管理员");
        }
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.ok("用户已删除"));
    }
}
