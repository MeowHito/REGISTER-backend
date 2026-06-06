package com.actionth.membership.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ContextUtils {

    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    public Integer getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            return null;
        try {
            return Integer.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Tries SecurityContext first, falls back to manually parsing the JWT cookie.
     * Useful for public-api endpoints where the JWT filter is skipped.
     */
    public Integer resolveUserIdFromCookie() {
        // Try SecurityContext first
        Integer id = getCurrentUserIdOrNull();
        if (id != null) return id;

        // Fall back: read JWT cookie from request
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;

            HttpServletRequest request = attrs.getRequest();
            Cookie[] cookies = request.getCookies();
            if (cookies == null) return null;

            String token = null;
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
            if (token == null || token.isBlank()) return null;

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return Integer.valueOf(claims.getSubject());
        } catch (Exception e) {
            log.debug("[ContextUtils] Could not resolve userId from cookie: {}", e.getMessage());
            return null;
        }
    }
}
