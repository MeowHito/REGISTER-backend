package com.actionth.membership.service;

import com.actionth.membership.model.dto.PermissionDto;
import java.util.List;

public interface PermissionService {

    List<PermissionDto> getPermissionsByRole(String roleUuid);

    void savePermissions(String roleId, List<PermissionDto> dtos);
}
