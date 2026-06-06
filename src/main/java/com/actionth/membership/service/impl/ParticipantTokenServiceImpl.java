package com.actionth.membership.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.actionth.membership.service.ParticipantTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipantTokenServiceImpl implements ParticipantTokenService {
    
    @Value("${participant-token.secret-key}")
    private String secret;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String createToken(String eventKey, String participantUuid, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject("participant-bib")
                .setIssuedAt(Date.from(now))
                .claim("eid", eventKey)
                .claim("pid", participantUuid)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String resolveParticipantId(String eventId, String token) {
        String eventKey = trimToNull(eventId);
        String t = trimToNull(token);
        if (eventKey == null || t == null) throw new IllegalArgumentException("Missing params");

        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(t)
                    .getBody();
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Invalid token");
        }

        String eid = String.valueOf(claims.get("eid"));
        String pid = String.valueOf(claims.get("pid"));

        if (!eventKey.equalsIgnoreCase(eid)) {
            throw new IllegalArgumentException("Token-event mismatch");
        }

        return pid;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String x = s.trim();
        return x.isEmpty() ? null : x;
    }
}
