package com.actionth.membership.utils;

import com.actionth.membership.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.actionth.membership.repository.CouponRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponCodeGenerator {

    private final CouponRepository couponRepository;

    public String generateUniqueCouponCodeChecked() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            String code = generateUniqueCouponCode();
            if (!couponRepository.existsByCouponCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique coupon code after " + maxAttempts + " attempts.");
    }

    private static String generateUniqueCouponCode() {

        long timestamp = Instant.now().toEpochMilli();

        String randomComponent = generateRandomString(6);

        String rawCode = randomComponent + "-" + timestamp;

        byte[] hash = sha256(rawCode);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 8);
    }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }
}