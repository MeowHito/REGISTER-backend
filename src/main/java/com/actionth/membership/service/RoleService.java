package com.actionth.membership.service;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.RoleDto;
import org.springframework.data.domain.Page;

public interface RoleService {

    Page<RoleDto> findAll(PagingData paging, Boolean active);

    RoleDto getRoleByUuid(String uuid);

    RoleDto createRole(RoleDto dto);

    RoleDto updateRole(RoleDto dto);

    void deleteRole(String uuid, String mode);
}