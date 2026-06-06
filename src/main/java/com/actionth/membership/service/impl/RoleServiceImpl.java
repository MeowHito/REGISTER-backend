package com.actionth.membership.service.impl;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.Role;
import com.actionth.membership.model.dto.PermissionDto;
import com.actionth.membership.model.dto.RoleDto;
import com.actionth.membership.repository.RoleRepository;
import com.actionth.membership.service.RoleService;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;

    @Override
    public Page<RoleDto> findAll(PagingData paging, Boolean active) {
        Specification<Role> activeSpec = (root, query, cb) -> {
            if (active != null) {
                return cb.equal(root.get("active"), active);
            } else {
                return null;
            }
        };

        Specification<Role> searchSpec = (root, query, cb) -> {
            if (paging != null && paging.getSearchField() != null && paging.getSearchText() != null) {
                String pattern = "%" + paging.getSearchText().toLowerCase() + "%";
                return cb.like(cb.lower(root.get(paging.getSearchField())), pattern);
            }
            return null;
        };

        Specification<Role> combinedSpec = Specification.where(activeSpec).and(searchSpec);

        if (paging == null) {
            List<RoleDto> dtos = roleRepository.findAll(combinedSpec)
                    .stream()
                    .map(this::mapRoleToDto)
                    .toList();
            return new PageImpl<>(dtos);
        } else {
            Pageable pageable = PageRequest.of(
                    paging.getPage(),
                    paging.getSize(),
                    paging.getSortField() != null
                            ? Sort.by(Sort.Direction.fromString(paging.getSortDirection()), paging.getSortField())
                            : Sort.unsorted());

            return roleRepository.findAll(combinedSpec, pageable)
                    .map(this::mapRoleToDto);
        }
    }

    @Override
    public RoleDto getRoleByUuid(String uuid) {
        Role role = roleRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with uuid: " + uuid));
        return mapRoleToDto(role);
    }

    @Override
    public RoleDto createRole(RoleDto dto) {
        Role role = modelMapper.map(dto, Role.class);
        role.setActive(true);
        return mapRoleToDto(roleRepository.save(role));
    }

    @Override
    public RoleDto updateRole(RoleDto dto) {
        Role role = roleRepository.findByUuid(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        role.setRoleType(dto.getRoleType());
        role.setRole(dto.getRole());

        return mapRoleToDto(roleRepository.save(role));
    }

    @Override
    public void deleteRole(String uuid, String mode) {
        Role role = roleRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        if ("soft".equalsIgnoreCase(mode)) {
            role.setActive(false);
            roleRepository.save(role);
        } else {
            roleRepository.delete(role);
        }
    }

    private RoleDto mapRoleToDto(Role role) {
        RoleDto roleDto = modelMapper.map(role, RoleDto.class);
        roleDto.setId(role.getUuid());

        if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            List<PermissionDto> permissionDtos = role.getPermissions().stream()
                    .map(permission -> {
                        PermissionDto dto = modelMapper.map(permission, PermissionDto.class);
                        dto.setRoleId(null);
                        dto.setId(permission.getUuid());
                        if (permission.getMenu() != null) {
                            dto.setMenuId(permission.getMenu().getUuid());
                        }
                        return dto;
                    })
                    .toList();

            roleDto.setPermissions(permissionDtos);
        }

        return roleDto;
    }
}
