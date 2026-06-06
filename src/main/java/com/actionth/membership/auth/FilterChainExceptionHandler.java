package com.actionth.membership.auth;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.actionth.membership.constant.Constants;
import com.actionth.membership.response.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FilterChainExceptionHandler extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public FilterChainExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            Response<Object> returnResponse;
            if ("Invalid Token".equals(e.getMessage())) {
                response.setStatus(400);
                returnResponse = Response.builder().success(false).message(Constants.ERROR_INVALID_TOKEN).build();
            } else {
                response.setStatus(500);
                returnResponse = Response.builder().success(false).message(Constants.ERROR_INVALID_TOKEN).build();
            }
            response.addHeader("content-type", "application/json");
            PrintWriter out = response.getWriter();
            out.write(objectMapper.writeValueAsString(returnResponse));
            out.flush();
        }
    }

}
