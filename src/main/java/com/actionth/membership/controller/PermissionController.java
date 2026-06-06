package com.actionth.membership.controller;

import com.actionth.membership.model.dto.PermissionDto;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.PermissionService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/role/{roleUuid}")
    public Response<List<PermissionDto>> getPermissions(@PathVariable String roleUuid) {
        return new Response<>(permissionService.getPermissionsByRole(roleUuid), "Success", true);
    }

    @PutMapping("/role/{roleUuid}")
    public Response<Void> updatePermissions(@PathVariable String roleUuid,
            @RequestBody List<PermissionDto> permissions) {
        permissionService.savePermissions(roleUuid, permissions);
        return new Response<>(null, "Updated", true);
    }
}
