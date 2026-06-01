package com.wltogether.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationCodeStore {

    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_INTERVAL_SECONDS = 60;

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSentTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> dailyCount = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String generate(String email) {
        String code = String.format("%06d", random.nextInt(1000000));
        codes.put(email, new CodeEntry(code, MAX_ATTEMPTS, System.currentTimeMillis()));
        lastSentTime.put(email, System.currentTimeMillis());
        dailyCount.merge(email, 1, Integer::sum);
        return code;
    }

    public boolean verify(String email, String inputCode) {
        CodeEntry entry = codes.get(email);
        if (entry == null) return false;

        if (isExpired(email)) {
            codes.remove(email);
            return false;
        }

        if (!MessageDigest.isEqual(entry.code.getBytes(), inputCode.getBytes())) {
            entry.attempts--;
            if (entry.attempts <= 0) {
                codes.remove(email);
            }
            return false;
        }

        codes.remove(email);
        return true;
    }

    public boolean isExpired(String email) {
        CodeEntry entry = codes.get(email);
        if (entry == null) return false;
        return System.currentTimeMillis() - entry.createdAt > CODE_TTL_MINUTES * 60 * 1000;
    }

    public boolean isExhausted(String email) {
        return codes.get(email) == null && lastSentTime.containsKey(email);
    }

    public int remainingAttempts(String email) {
        CodeEntry entry = codes.get(email);
        return entry != null ? entry.attempts : 0;
    }

    public long secondsUntilResendAllowed(String email) {
        Long last = lastSentTime.get(email);
        if (last == null) return 0;
        long elapsed = (System.currentTimeMillis() - last) / 1000;
        return Math.max(0, RESEND_INTERVAL_SECONDS - elapsed);
    }

    public boolean canResend(String email) {
        return secondsUntilResendAllowed(email) == 0;
    }

    public int dailyCount(String email) {
        return dailyCount.getOrDefault(email, 0);
    }

    public void remove(String email) {
        codes.remove(email);
    }

    private static class CodeEntry {
        final String code;
        int attempts;
        final long createdAt;

        CodeEntry(String code, int attempts, long createdAt) {
            this.code = code;
            this.attempts = attempts;
            this.createdAt = createdAt;
        }
    }
}
