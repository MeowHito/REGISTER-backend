package com.actionth.membership.service.impl;

import com.actionth.membership.model.Menu;
import com.actionth.membership.model.Permission;
import com.actionth.membership.model.Role;
import com.actionth.membership.model.dto.PermissionDto;
import com.actionth.membership.repository.MenuRepository;
import com.actionth.membership.repository.PermissionRepository;
import com.actionth.membership.repository.RoleRepository;
import com.actionth.membership.service.PermissionService;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionServiceImpl implements PermissionService {

	private final PermissionRepository permissionRepository;
	private final RoleRepository roleRepository;
	private final MenuRepository menuRepository;
	private final ModelMapper modelMapper;

	@Override
	public List<PermissionDto> getPermissionsByRole(String roleUuid) {
		Role role = roleRepository.findByUuid(roleUuid)
				.orElseThrow(() -> new EntityNotFoundException("Role not found"));

		return role.getPermissions().stream()
				.map(permission -> {
					PermissionDto dto = modelMapper.map(permission, PermissionDto.class);
					dto.setId(permission.getUuid());
					dto.setRoleId(permission.getRole().getUuid());
					dto.setMenuId(permission.getMenu().getUuid());
					return dto;
				})
				.toList();
	}

	@Override
	public void savePermissions(String roleId, List<PermissionDto> dtos) {
		Map<String, Menu> menuMap = new HashMap<>();

		List<Permission> permissions = new ArrayList<>();

		for (PermissionDto dto : dtos) {
			Permission permission = dto.getId() != null
					? permissionRepository.findByUuid(dto.getId())
							.orElse(new Permission())
					: new Permission();

			Role role = roleRepository.findByUuid(roleId)
					.orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

			Menu menu = menuMap.computeIfAbsent(dto.getMenuId(), id -> menuRepository.findByUuid(id)
					.orElseThrow(() -> new EntityNotFoundException("Menu not found: " + id)));

			permission.setRole(role);
			permission.setMenu(menu);
			permission.setCanRead(Optional.ofNullable(dto.getCanRead()).orElse(false));
			permission.setCanCreate(Optional.ofNullable(dto.getCanCreate()).orElse(false));
			permission.setCanUpdate(Optional.ofNullable(dto.getCanUpdate()).orElse(false));
			permission.setCanDelete(Optional.ofNullable(dto.getCanDelete()).orElse(false));

			permissions.add(permission);
		}

		permissionRepository.saveAll(permissions);
	}
}
