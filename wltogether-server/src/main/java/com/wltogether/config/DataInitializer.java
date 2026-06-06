package com.wltogether.config;

import com.wltogether.model.entity.User;
import com.wltogether.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.init-email:admin@wltogether.local}")
    private String adminEmail;

    @Value("${app.admin.init-password:}")
    private String adminPassword;

    @Value("${app.admin.init-username:admin}")
    private String adminUsername;

    private static final String DEFAULT_PASSWORD = "admin123";

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String password = resolvePassword();

        // Guard 1: no password configured → skip
        if (password == null || password.isBlank()) {
            log.info("未配置管理员初始密码（app.admin.init-password），跳过初始化");
            return;
        }

        // Guard 2: any admin already exists → skip (prevents duplicate admin creation)
        if (userRepository.existsByRole("ADMIN")) {
            log.info("系统已存在管理员账号，跳过初始化");
            return;
        }

        // Guard 3: configured email/username taken by non-admin user → abort with clear error
        if (userRepository.existsByEmail(adminEmail)) {
            log.error("管理员邮箱 {} 已被非管理员用户占用，无法初始化。请更换 app.admin.init-email 或手动处理。", adminEmail);
            return;
        }
        if (userRepository.existsByUsername(adminUsername)) {
            log.error("管理员用户名 {} 已被非管理员用户占用，无法初始化。请更换 app.admin.init-username 或手动处理。", adminUsername);
            return;
        }

        // Create admin
        try {
            User admin = User.builder()
                    .email(adminEmail)
                    .username(adminUsername)
                    .nickname("系统管理员")
                    .passwordHash(passwordEncoder.encode(password))
                    .role("ADMIN")
                    .status("ACTIVE")
                    .emailVerified(true)
                    .uid(generateUid())
                    .createdAt(Instant.now())
                    .build();

            userRepository.save(admin);
            log.info("============================================================");
            log.info("  初始管理员已创建:");
            log.info("  邮箱:     {}", adminEmail);
            log.info("  用户名:   {}", adminUsername);
            if (DEFAULT_PASSWORD.equals(password)) {
                log.warn("  ⚠️  密码为默认值 \"{}\"，请立即登录后台修改！", DEFAULT_PASSWORD);
            }
            log.info("============================================================");

        } catch (DataIntegrityViolationException e) {
            // Race condition guard: another instance created the admin between our check and insert
            log.warn("管理员创建冲突（可能多实例同时启动），已由另一实例创建，跳过");
        }
    }

    private String resolvePassword() {
        if (adminPassword != null && !adminPassword.isBlank()) {
            return adminPassword;
        }
        return null;
    }

    private String generateUid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
