package com.actionth.membership.controller;

import com.actionth.membership.model.dto.UserDto;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserServiceImpl userService;

    @GetMapping("/me")
    public Response<UserDto> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return new Response<>(null, "Unauthorized", false);
        }

        try {
            Integer userId = Integer.valueOf(authentication.getName());
            UserDto user = userService.getById(userId);
            return new Response<>(user, "OK", true);
        } catch (NumberFormatException e) {
            return new Response<>(null, "Invalid principal", false);
        } catch (Exception e) {
            return new Response<>(null, "User not found", false);
        }
    }
}
