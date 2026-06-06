package com.actionth.membership.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class JWTTokenVerifier extends OncePerRequestFilter {

    private final String key;
    private static final org.springframework.util.AntPathMatcher PM = new org.springframework.util.AntPathMatcher();
    private static final List<String> WHITELIST = List.of(
        "/public-api/**", "/login", "/actuator/health",
        "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**",
        "/favicon.ico", "/assets/**", "/css/**", "/js/**"
    );

    public JWTTokenVerifier(String key) { this.key = key; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        return WHITELIST.stream().anyMatch(p -> PM.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String token = null;

        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie c : cookies) {
                if ("access_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            }
        }

        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(key.getBytes())
                    .build()
                    .parseClaimsJws(token);

            Claims body = claimsJws.getBody();
            String userId = body.getSubject();

            Object authoritiesObj = body.get("authorities");
            List<GrantedAuthority> auths = new ArrayList<>();
            if (authoritiesObj instanceof List) {
                @SuppressWarnings("unchecked")
                var list = (List<Map<String, String>>) authoritiesObj;
                for (var m : list) auths.add(new SimpleGrantedAuthority(m.get("authority")));
            } else if (authoritiesObj instanceof String) {
                auths.add(new SimpleGrantedAuthority((String) authoritiesObj));
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, auths);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException e) {
            throw new IllegalStateException("Invalid Token");
        }

        chain.doFilter(request, response);
    }
}

