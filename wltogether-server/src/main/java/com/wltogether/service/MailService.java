package com.wltogether.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendVerificationCode(String to, String code) {
        String subject = "WLTogether 邮箱验证码";
        String body = buildVerificationEmail(code);
        send(to, subject, body);
    }

    public void sendResetCode(String to, String code) {
        String subject = "WLTogether 密码重置";
        String body = buildResetEmail(code);
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        if (from == null || from.isEmpty()) {
            log.warn("SMTP not configured — logging email to {} with body:\n{}", to, body);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试", e);
        }
    }

    private String buildVerificationEmail(String code) {
        return String.format("""
                你正在注册 WLTogether，验证码：

                \t\t%s

                5 分钟内有效。如非本人操作请忽略此邮件。
                """, formatCode(code));
    }

    private String buildResetEmail(String code) {
        return String.format("""
                你正在重置 WLTogether 账号密码，验证码：

                \t\t%s

                5 分钟内有效。如非本人操作请忽略此邮件。
                """, formatCode(code));
    }

    private String formatCode(String code) {
        return String.join(" ", code.split(""));
    }
}
