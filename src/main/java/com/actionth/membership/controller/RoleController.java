package com.actionth.membership.controller;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.PermissionDto;
import com.actionth.membership.model.dto.RoleDto;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.PermissionService;
import com.actionth.membership.service.RoleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @GetMapping
    public Response<Page<RoleDto>> getAllRole(
            @RequestParam(value = "paging", required = false) String pagingJson,
            @RequestParam(value = "active", required = false) Boolean active) throws JsonProcessingException {

        PagingData paging = null;
        if (pagingJson != null) {
            paging = new ObjectMapper().readValue(pagingJson, PagingData.class);
        }
        return new Response<>(roleService.findAll(paging, active), "Success", true);
    }

    @GetMapping("/{uuid}")
    public Response<RoleDto> getRole(@PathVariable String uuid) {
        return new Response<>(roleService.getRoleByUuid(uuid), "Success", true);
    }

    @PostMapping
    public Response<RoleDto> createRole(@RequestBody RoleDto dto) {
        return new Response<>(roleService.createRole(dto), "Created", true);
    }

    @PutMapping
    public Response<RoleDto> updateRole(@RequestBody RoleDto dto) {
        return new Response<>(roleService.updateRole(dto), "Updated", true);
    }

    @DeleteMapping("/{uuid}")
    public Response<Void> deleteRole(@PathVariable String uuid, @RequestParam(defaultValue = "soft") String mode) {
        roleService.deleteRole(uuid, mode);
        return new Response<>(null, "Deleted", true);
    }

    @GetMapping("/{uuid}/permissions")
    public Response<List<PermissionDto>> getPermissions(@PathVariable String uuid) {
        return new Response<>(permissionService.getPermissionsByRole(uuid), "Permissions retrieved", true);
    }
}
