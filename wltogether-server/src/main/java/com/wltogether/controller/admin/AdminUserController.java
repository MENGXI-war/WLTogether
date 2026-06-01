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

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<User>> list(Pageable pageable,
                                            @RequestParam(required = false) String status) {
        // Simplified: use findAll with filtering
        // For production, use specification or custom query
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    @PutMapping("/{id}/disable")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setStatus("DISABLED".equals(user.getStatus()) ? "ACTIVE" : "DISABLED");
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
