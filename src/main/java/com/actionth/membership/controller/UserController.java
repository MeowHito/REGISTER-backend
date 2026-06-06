package com.actionth.membership.controller;

import com.actionth.membership.model.dto.UserDto;
import com.actionth.membership.model.dto.UserViewDto;
import com.actionth.membership.model.request.GeneralRequest;
import com.actionth.membership.model.request.UseRoleDtoRequest;
import com.actionth.membership.model.request.UserAccountDTORequest;
import com.actionth.membership.model.request.UserProfileDTORequest;
import com.actionth.membership.model.request.UserRegisterDTORequest;
import com.actionth.membership.model.request.UserResetPasswordDTORequest;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.impl.UserServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    @PostMapping("/getAllUsers")
    public Response<Page<UserViewDto>> getAllUsers(@RequestBody GeneralRequest generalRequest) throws JsonProcessingException {
        return new Response<>(userService.findAll(generalRequest), "User retrieved successfully", true);
    }

    @PostMapping
    public Response<UserDto> createUser(@RequestBody UserRegisterDTORequest user) {
        return new Response<>(userService.createUser(user), "User created successfully", true);
    }

    @PutMapping
    public Response<UserDto> updateUser(@RequestBody UserProfileDTORequest user) {
        return new Response<>(userService.updateUser(user), "User updated successfully", true);
    }

    @DeleteMapping("/{uuid}")
    public Response<Void> deleteUser(@PathVariable String uuid, @RequestParam(value = "mode") String mode) {
        userService.deleteUser(uuid, mode);
        return new Response<>(null, "User deleted successfully", true);
    }

    @GetMapping("/{uuid}")
    public Response<UserDto> getUserByUuid(@PathVariable String uuid) {
        return new Response<>(userService.getByUuid(uuid), "Users retrieved successfully", true);
    }

    @PutMapping("/updateRole")
    public Response<Void> updateRole(@RequestBody UseRoleDtoRequest request) {
        userService.updateRole(request);
        return new Response<>(null, "Update role successfully", true);
    }

    @PutMapping("/updatePassword")
    public Response<UserDto> updatePassword( @RequestBody UserAccountDTORequest user) {
        return new Response<>(userService.updatePassword(user), "Update password successfully", true);
    }
    
    @PutMapping("/resetPassword")
    public Response<UserDto> resetPassword( @RequestBody UserResetPasswordDTORequest user) {
        return new Response<>(userService.resetPassword(user), "Reset password successfully", true);
    }
    
    @PutMapping("/updateStatus")
    public Response<Void> updateStatus( @RequestBody UserProfileDTORequest user) {
        userService.updateStatus(user);
        return new Response<>(null, "Update status successfully", true);
    }

    @GetMapping("/getOrganizerActive")
    public Response<List<UserViewDto>> getOrganizerActive() {
        return new Response<>(userService.getUserActiveByRole("organizer"), "Users retrieved successfully", true);
    }
    
    @GetMapping("/getUserActiveByRole")
    public Response<List<UserViewDto>> getUserActiveByRole(@RequestParam(value = "role") String role) {
        return new Response<>(userService.getUserActiveByRole(role), "Users retrieved successfully", true);
    }
    
    @GetMapping("/getUserActiveByRoleType")
    public Response<List<UserViewDto>> getUserActiveByRoleType(@RequestParam(value = "role") String role) {
        return new Response<>(userService.getUserActiveByRoleType(role), "Users retrieved successfully", true);
    }
}
