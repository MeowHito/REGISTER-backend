package com.actionth.membership.jwt;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.actionth.membership.constant.Constants;
import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.response.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class JWTUsernameAndPasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final String appEnv;

    private final String key;

    private final AuthenticationManager authenticationManager;

    private final ObjectMapper mapper = new ObjectMapper();

    public JWTUsernameAndPasswordAuthenticationFilter(AuthenticationManager authenticationManager, String key, String appEnv) {
        this.authenticationManager = authenticationManager;
        this.key = key;
        this.appEnv = appEnv;
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        try {
            UsernameAndPasswordAuthenticationRequest authenticationRequest = new ObjectMapper()
                    .readValue(request.getInputStream(), UsernameAndPasswordAuthenticationRequest.class);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getUsername(),
                    authenticationRequest.getPassword());
            return authenticationManager.authenticate(authentication);
        } catch (IOException e) {
            if (!"Invalid Token".equals(e.getMessage())) {
                log.error("Spring Security Filter Chain Exception:", e);
            }
            throw new BusinessException("Error reading authentication request", e);
        }

    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {

        String token = Jwts.builder()
                .setSubject(authResult.getName())
                .claim("authorities", authResult.getAuthorities())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(14, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor(key.getBytes()))
                .compact();

        boolean isProd = !"DEV".equalsIgnoreCase(
                Optional.ofNullable(appEnv).orElse("DEV"));

        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(isProd)
                .sameSite("Lax") // ปรับตามความเหมาะสม Strict/Lax/None
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Response<Object> res = Response.builder()
                .success(true)
                .data(null)
                .build();

        response.setStatus(200);
        response.addHeader("content-type", "application/json; charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(new ObjectMapper().writeValueAsString(res));
            out.flush();
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {
        Response<Object> res = Response.builder().success(false).message(Constants.USER_NOT_FOUND).build();
        response.setCharacterEncoding("UTF-8");
        response.addHeader("content-type", "application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(mapper.writeValueAsString(res));
        out.flush();
    }

}
